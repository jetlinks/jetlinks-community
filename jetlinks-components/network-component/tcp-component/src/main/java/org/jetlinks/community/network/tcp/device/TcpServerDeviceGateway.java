package org.jetlinks.community.network.tcp.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.core.server.session.KeepOnlineSession;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Slf4j
class TcpServerDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    @Getter
    private final String id;

    private final TcpServer tcpServer;

    private final String protocol;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final DeviceSessionManager sessionManager;

    private final DeviceGatewayMonitor gatewayMonitor;

    private final LongAdder counter = new LongAdder();

    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();

    private Disposable disposable;

    public TcpServerDeviceGateway(String id,
                                  String protocol,
                                  ProtocolSupports supports,
                                  DeviceRegistry deviceRegistry,
                                  DecodedClientMessageHandler clientMessageHandler,
                                  DeviceSessionManager sessionManager,
                                  TcpServer tcpServer) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.supports = supports;
        this.tcpServer = tcpServer;
        this.clientMessageHandler = clientMessageHandler;
        this.sessionManager = sessionManager;
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


    class TcpConnection {
        final TcpClient client;
        final AtomicReference<Duration> keepaliveTimeout = new AtomicReference<>();
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;

        TcpConnection(TcpClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            gatewayMonitor.totalConnection(counter.sum());
            client.onDisconnect(() -> {
                counter.decrement();
                gatewayMonitor.disconnected();
                gatewayMonitor.totalConnection(counter.sum());
            });
            gatewayMonitor.connected();
            DeviceSession session = sessionManager.getSession(client.getId());
            if (session == null) {
                session = new UnknownTcpDeviceSession(client.getId(), client, getTransport()) {
                    @Override
                    public Mono<Boolean> send(EncodedMessage encodedMessage) {
                        return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                    }

                    @Override
                    public void setKeepAliveTimeout(Duration timeout) {
                        keepaliveTimeout.set(timeout);
                    }

                    @Override
                    public Optional<InetSocketAddress> getClientAddress() {
                        return Optional.of(address);
                    }
                };
            }

            sessionRef.set(session);

        }

        Mono<Void> accept() {
            return client
                .subscribe()
                .filter(tcp -> started.get())
                .publishOn(Schedulers.parallel())
                .flatMap(this::handleTcpMessage)
                .onErrorContinue((err, ignore) -> log.error(err.getMessage(), err))
                .then()
                .doOnCancel(client::shutdown);
        }

        Mono<Void> handleTcpMessage(TcpMessage message) {

            return getProtocol()
                .flatMap(pt -> pt.getMessageCodec(getTransport()))
                .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message,registry)))
                .cast(DeviceMessage.class)
                .doOnNext(msg-> gatewayMonitor.receivedMessage())
                .flatMap(this::handleDeviceMessage)
                .doOnEach(ReactiveLogger.onError(err ->
                    log.error("处理TCP[{}]消息失败:\n{}",
                        address,
                        message
                        , err)))
                .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                .then();
        }

        Mono<Void> handleDeviceMessage(DeviceMessage message) {
            return registry
                .getDevice(message.getDeviceId())
                .switchIfEmpty(Mono.defer(() -> {
                    if (processor.hasDownstreams()) {
                        sink.next(message);
                    }
                    if (message instanceof DeviceRegisterMessage) {
                        return clientMessageHandler
                            .handleMessage(null, message)
                            .then(Mono.empty());
                    } else {
                        log.warn("无法从tcp[{}]消息中获取设备信息:{}",address, message);
                        return Mono.empty();
                    }
                }))
                .flatMap(device -> {
                    DeviceSession fSession = sessionManager.getSession(device.getDeviceId());
                    //处理设备上线消息
                    if (fSession == null) {
                        boolean keepOnline = message.getHeader(Headers.keepOnline).orElse(false);
                        String sessionId = device.getDeviceId();
                        fSession = new TcpDeviceSession(sessionId, device, client, getTransport()) {
                            @Override
                            public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                            }
                        };
                        //保持设备一直在线.（短连接上报数据的场景.可以让设备一直为在线状态）
                        if (keepOnline) {
                            fSession = new KeepOnlineSession(fSession, Duration.ofMillis(-1));
                        } else {
                            client.onDisconnect(() -> sessionManager.unregister(device.getDeviceId()));
                        }
                        sessionRef.set(fSession);
                        sessionManager.register(fSession);
                    }
                    fSession.keepAlive();
                    Duration timeout = message.getHeader(Headers.keepOnlineTimeoutSeconds).map(Duration::ofSeconds).orElse(keepaliveTimeout.get());
                    if (timeout != null) {
                        fSession.setKeepAliveTimeout(timeout);
                    }
                    fSession.keepAlive();
                    if (message instanceof DeviceOnlineMessage) {
                        return Mono.empty();
                    }
                    //设备下线
                    if (message instanceof DeviceOfflineMessage) {
                        sessionManager.unregister(device.getDeviceId());
                        return Mono.empty();
                    }
                    message.addHeaderIfAbsent(Headers.clientAddress, String.valueOf(address));
                    if (processor.hasDownstreams()) {
                        sink.next(message);
                    }
                    return clientMessageHandler.handleMessage(device, message);
                })
                .then()
                ;
        }
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = tcpServer
            .handleConnection()
            .publishOn(Schedulers.parallel())
            .flatMap(client -> new TcpConnection(client).accept(), Integer.MAX_VALUE)
            .subscriberContext(ReactiveLogger.start("network", tcpServer.getId()))
            .subscribe(
                ignore -> {
                },
                error -> log.error(error.getMessage(), error)
            );
    }

    @Override
    public Flux<Message> onMessage() {
        return processor.map(Function.identity());
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> started.set(false));
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            started.set(false);
            disposable.dispose();
            disposable = null;
        });
    }

    @Override
    public boolean isAlive() {
        return started.get();
    }
}
