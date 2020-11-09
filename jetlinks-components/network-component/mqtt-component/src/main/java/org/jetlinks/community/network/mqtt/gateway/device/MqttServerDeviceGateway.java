package org.jetlinks.community.network.mqtt.gateway.device;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.MqttAuthenticationRequest;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttConnectionSession;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.core.server.session.KeepOnlineSession;
import org.jetlinks.core.server.session.ReplaceableDeviceSession;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Slf4j
class MqttServerDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    @Getter
    private final String id;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final MqttServer mqttServer;

    private final DecodedClientMessageHandler messageHandler;

    private final DeviceGatewayMonitor gatewayMonitor;

    private final LongAdder counter = new LongAdder();

    private final EmitterProcessor<Message> messageProcessor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();

    private final Mono<ProtocolSupport> supportMono;

    private Disposable disposable;

    public MqttServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   DeviceSessionManager sessionManager,
                                   MqttServer mqttServer,
                                   DecodedClientMessageHandler messageHandler,
                                   Mono<ProtocolSupport> customProtocol
    ) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.mqttServer = mqttServer;
        this.messageHandler = messageHandler;
        this.supportMono = customProtocol;
    }

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
            .publishOn(Schedulers.parallel())
            .flatMap(this::handleConnection)
            .flatMap(tuple3 -> handleAuthResponse(tuple3.getT1(), tuple3.getT2(), tuple3.getT3()))
            .flatMap(tp -> handleAcceptedMqttConnection(tp.getT1(), tp.getT2(), tp.getT3()), Integer.MAX_VALUE)
            .onErrorContinue((err, obj) -> log.error("处理MQTT连接失败", err))
            .subscriberContext(ReactiveLogger.start("network", mqttServer.getId()))
            .subscribe();

    }

    //处理连接，并进行认证
    private Mono<Tuple3<DeviceOperator, AuthenticationResponse, MqttConnection>> handleConnection(MqttConnection connection) {

        return Mono
            .justOrEmpty(connection.getAuth())
            .flatMap(auth -> {
                MqttAuthenticationRequest request = new MqttAuthenticationRequest(connection.getClientId(), auth.getUsername(), auth.getPassword(), getTransport());
                return supportMono
                    //使用自定义协议来认证
                    .map(support -> support.authenticate(request, registry))
                    .defaultIfEmpty(Mono.defer(() -> registry
                        .getDevice(connection.getClientId())
                        .flatMap(device -> device.authenticate(request))))
                    .flatMap(Function.identity())
                    .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)));
            })
            .flatMap(resp -> {
                String deviceId = StringUtils.isEmpty(resp.getDeviceId()) ? connection.getClientId() : resp.getDeviceId();
                //认证返回了新的设备ID,则使用新的设备
                return registry
                    .getDevice(deviceId)
                    .map(operator -> Tuples.of(operator, resp, connection))
                    .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)))
                    ;
            })
            //设备注册信息不存在,拒绝连接
            .onErrorResume((err) -> Mono.fromRunnable(() -> {
                gatewayMonitor.rejected();
                connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                log.error("MQTT连接认证[{}]失败", connection.getClientId(), err);
            }));
    }

    //处理认证结果
    private Mono<Tuple3<MqttConnection, DeviceOperator, MqttConnectionSession>> handleAuthResponse(DeviceOperator device,
                                                                                                   AuthenticationResponse resp,
                                                                                                   MqttConnection connection) {
        return Mono
            .fromCallable(() -> {
                String deviceId = device.getDeviceId();
                if (resp.isSuccess()) {
                    counter.increment();
                    DeviceSession session = sessionManager.getSession(deviceId);
                    MqttConnectionSession newSession = new MqttConnectionSession(deviceId, device, getTransport(), connection, gatewayMonitor);
                    if (null == session) {
                        sessionManager.register(newSession);
                    } else if (session instanceof ReplaceableDeviceSession) {
                        ((ReplaceableDeviceSession) session).replaceWith(newSession);
                    }
                    gatewayMonitor.connected();
                    gatewayMonitor.totalConnection(counter.sum());
                    //监听断开连接
                    connection.onClose(conn -> {
                        counter.decrement();
                        DeviceSession _tmp = sessionManager.getSession(newSession.getId());

                        if (newSession == _tmp || _tmp == null) {
                            sessionManager.unregister(deviceId);
                        }
                        gatewayMonitor.disconnected();
                        gatewayMonitor.totalConnection(counter.sum());
                    });
                    return Tuples.of(connection.accept(), device, newSession);
                } else {
                    connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                    gatewayMonitor.rejected();
                    log.warn("MQTT客户端认证[{}]失败:{}", deviceId, resp.getMessage());
                }
                return null;
            })
            .onErrorResume(error -> Mono.fromRunnable(() -> {
                log.error(error.getMessage(), error);
                gatewayMonitor.rejected();
                connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
            }));
    }

    //处理已经建立连接的MQTT连接
    private Mono<Void> handleAcceptedMqttConnection(MqttConnection connection, DeviceOperator operator, MqttConnectionSession session) {

        return connection
            .handleMessage()
            .filter(pb -> started.get())
            .doOnCancel(() -> {
                //流被取消时(可能网关关闭了)断开连接
                connection.close().subscribe();
            })
            .publishOn(Schedulers.parallel())
            .doOnNext(msg -> gatewayMonitor.receivedMessage())
            .flatMap(publishing ->
                this.decodeAndHandleMessage(operator, session, publishing.getMessage(), connection)
                    //ack
                    .doOnSuccess(s -> publishing.acknowledge())
            )
            //合并遗言消息
            .mergeWith(
                Mono.justOrEmpty(connection.getWillMessage())
                    .flatMap(mqttMessage -> this.decodeAndHandleMessage(operator, session, mqttMessage, connection))
            )
            .subscriberContext(ReactiveLogger.start("network", mqttServer.getId()))
            .then();
    }

    //解码消息并处理
    private Mono<Void> decodeAndHandleMessage(DeviceOperator operator,
                                              MqttConnectionSession session,
                                              MqttMessage message,
                                              MqttConnection connection) {
        return operator
            .getProtocol()
            .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
            .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(session, message,registry)))
            .cast(DeviceMessage.class)
            .flatMap(msg -> {
                if (msg instanceof CommonDeviceMessage) {
                    CommonDeviceMessage _msg = ((CommonDeviceMessage) msg);
                    if (StringUtils.isEmpty(_msg.getDeviceId())) {
                        _msg.setDeviceId(operator.getDeviceId());
                    }
                }
                String deviceId = msg.getDeviceId();
                if (!StringUtils.isEmpty(deviceId)) {
                    //返回了其他设备的消息,则自动创建会话
                    if (!deviceId.equals(operator.getDeviceId())) {
                        DeviceSession anotherSession = sessionManager.getSession(msg.getDeviceId());
                        if (anotherSession == null) {
                            return registry
                                .getDevice(msg.getDeviceId())
                                .map(device -> handleMessage(device.getDeviceId(), device, msg, session))
                                .defaultIfEmpty(Mono.defer(()->handleMessage(msg.getDeviceId(), operator, msg, session)))
                                .flatMap(Function.identity());
                        }
                    }
                }
                return handleMessage(deviceId, operator, msg, session);
            })
            .then()
            .doOnEach(ReactiveLogger.onError(err -> log.error("处理MQTT连接[{}]消息失败:{}", operator.getDeviceId(), message, err)))
            .onErrorResume((err) -> Mono.empty())//发生错误不中断流
            ;
    }

    private Mono<Void> handleMessage(String deviceId,
                                     DeviceOperator device,
                                     DeviceMessage message,
                                     MqttConnectionSession firstSession) {
        DeviceSession managedSession = sessionManager.getSession(deviceId);

        //主动离线
        if (message instanceof DeviceOfflineMessage) {
            sessionManager.unregister(deviceId);
            return Mono.empty();
        }

        //session 不存在,可能是同一个mqtt返回多个设备消息
        if (managedSession == null) {
            firstSession = new MqttConnectionSession(deviceId, device, getTransport(), firstSession.getConnection(), gatewayMonitor);
            sessionManager.register(managedSession = firstSession);
        }

        //保持会话，在低功率设备上,可能无法保持mqtt长连接.
        if (message.getHeader(Headers.keepOnline).orElse(false)) {
            if (!managedSession.isWrapFrom(KeepOnlineSession.class)) {
                int timeout = message.getHeaderOrDefault(Headers.keepOnlineTimeoutSeconds);
                KeepOnlineSession keepOnlineSession = new KeepOnlineSession(firstSession, Duration.ofSeconds(timeout));
                //替换会话
                managedSession = sessionManager.replace(firstSession, keepOnlineSession);
            }
        } else {
            managedSession = firstSession;
        }

        managedSession.keepAlive();

        if (messageProcessor.hasDownstreams()) {
            sink.next(message);
        }
        if (message instanceof DeviceOnlineMessage) {
            return Mono.empty();
        }

        return messageHandler
            .handleMessage(device, message)
            .then();
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
