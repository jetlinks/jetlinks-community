package org.jetlinks.community.network.tcp.device;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.community.network.utils.DeviceGatewayHelper;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
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
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j(topic = "system.tcp.gateway")
class TcpServerDeviceGateway extends AbstractDeviceGateway implements MonitorSupportDeviceGateway {

    private final TcpServer tcpServer;

    private final String protocol;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final org.jetlinks.core.device.session.DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    public TcpServerDeviceGateway(String id,
                                  String protocol,
                                  ProtocolSupports supports,
                                  DeviceRegistry deviceRegistry,
                                  DecodedClientMessageHandler clientMessageHandler,
                                  DeviceSessionManager sessionManager,
                                  TcpServer tcpServer) {
        super(id);
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.supports = supports;
        this.tcpServer = tcpServer;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    public Mono<ProtocolSupport> getProtocol() {
        return supports.getProtocol(protocol);
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.TCP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }


    class TcpConnection implements DeviceGatewayContext {
        final TcpClient client;
        final AtomicReference<Duration> keepaliveTimeout = new AtomicReference<>();
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;

        TcpConnection(TcpClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            monitor.totalConnection(counter.sum());
            client.onDisconnect(() -> {
                counter.decrement();
                monitor.disconnected();
                monitor.totalConnection(counter.sum());
                //check session
                sessionManager
                    .getSession(client.getId())
                    .subscribe();
            });
            monitor.connected();
            sessionRef.set(new UnknownTcpDeviceSession(client.getId(), client, getTransport()));
        }

        Mono<Void> accept() {
            return getProtocol()
                .flatMap(protocol -> protocol.onClientConnect(getTransport(), client, this))
                .then(
                    client
                        .subscribe()
                        .filter(tcp -> isStarted())
                        .flatMap(this::handleTcpMessage)
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
            long time = System.nanoTime();
            return getProtocol()
                .flatMap(pt -> pt.getMessageCodec(getTransport()))
                .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message, registry)))
                .cast(DeviceMessage.class)
                .flatMap(msg -> this
                    .handleDeviceMessage(msg)
                    .as(MonoTracer.create(
                        DeviceTracer.SpanName.decode(msg.getDeviceId()),
                        builder -> {
                            builder.setAttribute(DeviceTracer.SpanKey.message, msg.toString());
                            builder.setStartTimestamp(time, TimeUnit.NANOSECONDS);
                        })))
                .doOnEach(ReactiveLogger
                              .onError(err -> log.error("Handle TCP[{}] message failed:\n{}",
                                                        address,
                                                        message
                                  , err)))

                .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                .subscribeOn(Schedulers.parallel())
                .then();
        }

        Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message) {
            monitor.receivedMessage();
            return helper
                .handleDeviceMessage(message,
                                     device -> new TcpDeviceSession(device, client, getTransport(), monitor),
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

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = tcpServer
            .handleConnection()
            .publishOn(Schedulers.parallel())
            .flatMap(client -> new TcpConnection(client)
                         .accept()
                         .onErrorResume(err -> {
                             log.error("handle tcp client[{}] error", client.getRemoteAddress(), err);
                             return Mono.empty();
                         })
                , Integer.MAX_VALUE)
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .contextWrite(ReactiveLogger.start("network", tcpServer.getId()))
            .subscribe(
                ignore -> {
                },
                error -> log.error(error.getMessage(), error)
            );
    }

    @Override
    protected Mono<Void> doShutdown() {
        if (disposable != null) {
            disposable.dispose();
        }
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }
}
