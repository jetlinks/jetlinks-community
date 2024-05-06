package org.jetlinks.community.network.tcp.gateway.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
class TcpServerDeviceGateway extends AbstractDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    final TcpServer tcpServer;

    @Getter
    Mono<ProtocolSupport> protocol;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private final AtomicBoolean started = new AtomicBoolean();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    //连接检查超时时间,超过时间连接没有被正确处理返回会话,将被自动断开连接
    @Setter
    private Duration connectCheckTimeout = TimeUtils.parse(System.getProperty("gateway.tcp.network.connect-check-timeout", "10s"));

    public TcpServerDeviceGateway(String id,
                                  Mono<ProtocolSupport> protocol,
                                  DeviceRegistry deviceRegistry,
                                  DecodedClientMessageHandler clientMessageHandler,
                                  DeviceSessionManager sessionManager,
                                  TcpServer tcpServer) {
        super(id);
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.tcpServer = tcpServer;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }


    class TcpConnection implements DeviceGatewayContext {
        final TcpClient client;
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;
        Disposable legalityChecker;

        TcpConnection(TcpClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            monitor.totalConnection(counter.sum());
            client.onDisconnect(() -> {
                counter.decrement();
                monitor.disconnected();
                monitor.totalConnection(counter.sum());
                //check session
                DeviceSession session = sessionRef.get();
                if (session.getDeviceId() != null) {
                    sessionManager
                        .getSession(session.getDeviceId())
                        .subscribe();
                }

            });
            monitor.connected();

            sessionRef.set(new UnknownTcpDeviceSession(client.getId(), client, DefaultTransport.TCP, monitor));

            legalityChecker = Schedulers
                .parallel()
                .schedule(this::checkLegality, connectCheckTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public void checkLegality() {
            //超过时间还未获取到任何设备则认为连接不合法，自动断开连接
            if ((sessionRef.get() instanceof UnknownTcpDeviceSession)) {
                log.info("tcp [{}] connection is illegal, close it.", address);
                try {
                    client.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }

        Mono<Void> accept() {
            return getProtocol()
                .flatMap(protocol -> protocol.onClientConnect(DefaultTransport.TCP, client, this))
                .then(
                    client
                        .subscribe()
                        .filter(tcp -> started.get())
                        .concatMap(this::handleTcpMessage)
                        .onErrorResume((err) -> {
                            log.error(err.getMessage(), err);
                            client.shutdown();
                            return Mono.empty();
                        })
                        .then()
                )
                .doOnCancel(client::shutdown);
        }

        Mono<Void> handleTcpMessage(TcpMessage message) {
            return getProtocol()
                .flatMap(pt -> pt.getMessageCodec(DefaultTransport.TCP))
                .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(
                    sessionRef.get(), message, registry, msg -> handleDeviceMessage(msg).then())))
                .cast(DeviceMessage.class)
                .concatMap(msg -> this
                    .handleDeviceMessage(msg)
                    .as(MonoTracer.create(
                        DeviceTracer.SpanName.decode(msg.getDeviceId()),
                        (span, _msg) -> span.setAttributeLazy(DeviceTracer.SpanKey.message, _msg::toString))))
                .onErrorResume((err) -> {
                    log.error("Handle TCP[{}] message failed:\n{}",
                              address,
                              message
                        , err);
                    return Mono.fromRunnable(client::reset);
                })
                .subscribeOn(Schedulers.parallel())
                .then();
        }

        Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message) {
            Disposable checker = legalityChecker;
            if (checker != null) {
                checker.dispose();
                legalityChecker = null;
            }
            monitor.receivedMessage();
            return helper
                .handleDeviceMessage(
                    message,
                    device -> new TcpDeviceSession(device, client,DefaultTransport.TCP, monitor),
                    session -> {
                        TcpDeviceSession deviceSession = session.unwrap(TcpDeviceSession.class);
                        deviceSession.setClient(client);
                        sessionRef.set(deviceSession);
                    },
                    () -> log.warn("TCP{}: The device[{}] in the message body does not exist:{}", address, message.getDeviceId(), message)
                )
                .thenReturn(message);
        }

        @Override
        public Mono<DeviceOperator> getDevice(String deviceId) {
            return registry.getDevice(deviceId);
        }

        @Override
        public Mono<DeviceProductOperator> getProduct(String productId) {
            return registry.getProduct(productId);
        }

        @Override
        public Mono<Void> onMessage(DeviceMessage message) {
            return handleDeviceMessage(message).then();
        }
    }


    private void closeClient(TcpClient client) {
        try {
            client.shutdown();
        } catch (Throwable ignore) {

        }
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = tcpServer
            .handleConnection()
//            .onBackpressureBuffer(maxConcurrency, client -> {
//                log.warn("tcp server [{}] connection buffer is full, close it.", client.getRemoteAddress());
//                closeClient(client);
//            })
            .publishOn(Schedulers.parallel())
            .flatMap(client -> new TcpConnection(client)
                         .accept()
                         .onErrorResume(err -> {
                             log.error("handle tcp client[{}] error", client.getRemoteAddress(), err);
                             return Mono.empty();
                         })
                , Integer.MAX_VALUE)
            .contextWrite(ReactiveLogger.start("network", tcpServer.getId()))
            .subscribe(
                ignore -> {
                },
                error -> log.error(error.getMessage(), error)
            );
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    protected Mono<Void> doShutdown() {
        return Mono.fromRunnable(() -> {
            if (null != disposable) {
                disposable.dispose();
                disposable = null;
            }
        });
    }
}
