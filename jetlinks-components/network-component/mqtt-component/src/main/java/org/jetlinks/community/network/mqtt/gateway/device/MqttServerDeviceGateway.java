package org.jetlinks.community.network.mqtt.gateway.device;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.MqttAuthenticationRequest;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttConnectionSession;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Slf4j
class MqttServerDeviceGateway implements DeviceGateway , MonitorSupportDeviceGateway {

    @Getter
    private String id;

    private DeviceRegistry registry;

    private DeviceSessionManager sessionManager;

    private MqttServer mqttServer;

    private DecodedClientMessageHandler messageHandler;

    private DeviceGatewayMonitor gatewayMonitor;

    private LongAdder counter = new LongAdder();

    public MqttServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   DeviceSessionManager sessionManager,
                                   MqttServer mqttServer,
                                   DecodedClientMessageHandler messageHandler) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.mqttServer = mqttServer;
        this.messageHandler = messageHandler;
    }

    private EmitterProcessor<Message> messageProcessor = EmitterProcessor.create(false);

    private FluxSink<Message> sink = messageProcessor.sink();

    private AtomicBoolean started = new AtomicBoolean();

    private Disposable disposable;

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = mqttServer
            .handleConnection()
            .filter(conn -> {
                if (!started.get()) {
                    conn.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    gatewayMonitor.rejected();
                }
                return started.get();
            })
            .flatMap(con -> Mono.justOrEmpty(con.getAuth())
                //没有认证信息,则拒绝连接.
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    con.reject(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);
                    gatewayMonitor.rejected();
                }))
                .flatMap(auth ->
                    registry.getDevice(con.getClientId())
                        .flatMap(device -> device
                            .authenticate(new MqttAuthenticationRequest(con.getClientId(), auth.getUsername(), auth.getPassword(), getTransport()))
                            .switchIfEmpty(Mono.fromRunnable(() -> con.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)))
                            .flatMap(resp -> {
                                String deviceId = StringUtils.isEmpty(resp.getDeviceId()) ? device.getDeviceId() : resp.getDeviceId();
                                //认证返回了新的设备ID,则使用新的设备
                                if (!deviceId.equals(device.getDeviceId())) {
                                    return registry
                                        .getDevice(deviceId)
                                        .map(operator -> Tuples.of(operator, resp, con));
                                }
                                return Mono.just(Tuples.of(device, resp, con));
                            })
                        ))
                //设备注册信息不存在,拒绝连接
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    con.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                    gatewayMonitor.rejected();
                }))
                .onErrorResume((err) -> Mono.fromRunnable(() -> {
                    gatewayMonitor.rejected();
                    con.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    log.error("MQTT连接认证[{}]失败", con.getClientId(), err);
                })))
            .flatMap(tuple3 -> {
                counter.increment();
                DeviceOperator device = tuple3.getT1();
                AuthenticationResponse resp = tuple3.getT2();
                MqttConnection con = tuple3.getT3();
                String deviceId = device.getDeviceId();
                if (resp.isSuccess()) {
                    DeviceSession session = new MqttConnectionSession(deviceId, device, getTransport(), con) {
                        @Override
                        public Mono<Boolean> send(EncodedMessage encodedMessage) {
                            return super.send(encodedMessage).doOnSuccess(s -> gatewayMonitor.sentMessage());
                        }
                    };
                    sessionManager.register(session);
                    gatewayMonitor.connected();
                    gatewayMonitor.totalConnection(counter.sum());
                    //监听断开连接
                    con.onClose(conn -> {
                        counter.decrement();
                        sessionManager.unregister(deviceId);
                        gatewayMonitor.disconnected();
                        gatewayMonitor.totalConnection(counter.sum());
                    });
                    return Mono.just(Tuples.of(con.accept(), device, session));
                } else {
                    gatewayMonitor.rejected();
                    log.warn("MQTT客户端认证[{}]失败:{}", deviceId, resp.getMessage());
                }
                return Mono.empty();
            })
            .onErrorContinue((err,obj) -> log.error("处理MQTT连接失败", err))
            .subscribe(tp -> tp.getT1()
                .handleMessage()
                .filter(pb -> started.get())
                .takeWhile(pub -> disposable != null)
                .doOnNext(msg -> gatewayMonitor.receivedMessage())
                .flatMap(publishing -> tp.getT2()
                    .getProtocol()
                    .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
                    .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                        @Override
                        public DeviceSession getSession() {
                            return tp.getT3();
                        }

                        @Override
                        @Nonnull
                        public EncodedMessage getMessage() {
                            return publishing.getMessage();
                        }
                    }))
                    .flatMap(msg -> {
                        if (messageProcessor.hasDownstreams()) {
                            sink.next(msg);
                        }
                        return messageHandler.handleMessage(tp.getT2(), msg);
                    })
                    .onErrorResume((err) ->
                        Mono.fromRunnable(() -> log.error("处理MQTT连接[{}]消息失败:{}", tp.getT2().getDeviceId(), publishing.getMessage(), err))
                    ))
                .subscribe());

    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Override
    public Flux<Message> onMessage() {
        return messageProcessor
            .map(Function.identity());
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
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            disposable = null;
        });
    }

    @Override
    public boolean isAlive() {
        return started.get();
    }

}
