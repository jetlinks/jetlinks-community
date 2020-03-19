package org.jetlinks.community.network.tcp.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Slf4j
class TcpServerDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    @Getter
    private String id;

    private TcpServer tcpServer;

    private String protocol;

    private ProtocolSupports supports;

    private DeviceRegistry registry;

    private DecodedClientMessageHandler clientMessageHandler;

    private DeviceSessionManager sessionManager;

    private DeviceGatewayMonitor gatewayMonitor;

    private LongAdder counter = new LongAdder();

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


    private EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private FluxSink<Message> sink = processor.sink();

    private AtomicBoolean started = new AtomicBoolean();

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

    private List<Disposable> disposable = new CopyOnWriteArrayList<>();

    private void doStart() {
        if (started.getAndSet(true) || !disposable.isEmpty()) {
            return;
        }

        disposable.add(tcpServer
            .handleConnection()
            .flatMap(client -> {
                counter.increment();
                gatewayMonitor.totalConnection(counter.intValue());
                client.onDisconnect(() -> {
                    counter.decrement();
                    gatewayMonitor.disconnected();
                    gatewayMonitor.totalConnection(counter.sum());
                    sessionManager.unregister(client.getId());
                });
                AtomicReference<Duration> keepaliveTimeout = new AtomicReference<>();

                return client.subscribe()
                    .filter(r -> started.get())
                    .doOnNext(r -> gatewayMonitor.receivedMessage())
                    .flatMap(tcpMessage -> getProtocol()
                        .flatMap(pt -> pt.getMessageCodec(getTransport()))
                        .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                            @Override
                            public EncodedMessage getMessage() {
                                return tcpMessage;
                            }

                            @Override
                            public DeviceSession getSession() {
                                DeviceSession session = sessionManager.getSession(client.getId());
                                //session还未注册
                                if (session == null) {
                                    return new UnknownTcpDeviceSession(client.getId(), client, getTransport()) {
                                        @Override
                                        public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                            return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                                        }

                                        @Override
                                        public void setKeepAliveTimeout(Duration timeout) {
                                            keepaliveTimeout.set(timeout);
                                        }
                                    };
                                }
                                return session;
                            }

                            @Override
                            public DeviceOperator getDevice() {
                                return null;
                            }
                        })))
                    .cast(DeviceMessage.class)
                    .flatMap(message -> registry
                        .getDevice(message.getDeviceId())
                        .flatMap(device -> {
                            //设备上线
                            if (message instanceof DeviceOnlineMessage) {
                                TcpDeviceSession session = new TcpDeviceSession(client.getId(), device, client, getTransport()) {
                                    @Override
                                    public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                        return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                                    }
                                };
                                if (keepaliveTimeout.get() != null) {
                                    session.setKeepAliveTimeout(keepaliveTimeout.get());
                                }
                                sessionManager.register(session);
                                return Mono.empty();
                            }
                            //设备下线
                            if (message instanceof DeviceOfflineMessage) {
                                sessionManager.unregister(device.getDeviceId());
                                return Mono.empty();
                            }
                            if (processor.hasDownstreams()) {
                                sink.next(message);
                            }
                            return clientMessageHandler.handleMessage(device, message);
                        }))
                    .onErrorContinue((err, o) -> log.error(err.getMessage(), err));
            }).subscribe());
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

            disposable.forEach(Disposable::dispose);

            disposable.clear();
        });
    }

    @Override
    public boolean isAlive() {
        return started.get();
    }
}
