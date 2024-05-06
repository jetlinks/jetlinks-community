package org.jetlinks.community.network.mqtt.gateway.device;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttConnectionSession;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttPublishing;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.community.utils.SystemUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.KeepOnlineSession;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static org.jetlinks.core.trace.DeviceTracer.SpanKey;
import static org.jetlinks.core.trace.DeviceTracer.SpanName;

/**
 * MQTT 服务设备网关,用于通过内置的mqtt server来进行设备通信,接入设备到平台中.
 *
 * <pre>
 *     1. 监听Mqtt服务中的连接{@link MqttServer#handleConnection()}
 *     2. 使用{@link MqttConnection#getClientId()}作为设备ID,从设备注册中心中获取设备.
 *     3. 使用设备对应的协议{@link DeviceOperator#getProtocol()}来进行认证{@link ProtocolSupport#authenticate(AuthenticationRequest, DeviceOperator)}
 *     4. 认证通过后应答mqtt,注册会话{@link DeviceSessionManager#compute(String, Function)}.
 *     5. 监听mqtt消息推送,{@link MqttConnection#handleMessage()}
 *     6. 当收到消息时,调用对应设备使用的协议{@link ProtocolSupport#getMessageCodec(Transport)}进行解码{@link DeviceMessageCodec#decode(MessageDecodeContext)}
 * </pre>
 *
 * @author zhouhao
 * @see MqttServer
 * @see ProtocolSupport
 * @since 1.0
 */
@Slf4j
class MqttServerDeviceGateway extends AbstractDeviceGateway {
    static AttributeKey<String> clientId = AttributeKey.stringKey("clientId");

    static AttributeKey<String> username = AttributeKey.stringKey("username");

    static AttributeKey<String> password = AttributeKey.stringKey("password");


    //设备注册中心
    private final DeviceRegistry registry;

    //设备会话管理器
    private final DeviceSessionManager sessionManager;

    //Mqtt 服务
    @Getter
    private final MqttServer mqttServer;

    //解码后的设备消息处理器
    private final DecodedClientMessageHandler messageHandler;

    //连接计数器
    private final LongAdder counter = new LongAdder();

    //自定义的认证协议,在设备网关里配置自定义的认证协议来进行统一的设备认证处理
    //场景: 默认情况下时使用mqtt的clientId作为设备ID来进行设备与连接的绑定的,如果clientId的规则比较复杂
    //或者需要使用其他的clientId规则，则可以指定自定义的认证协议来进行认证.
    //指定了自定义协议的局限是: 所有使用同一个mqtt服务接入的设备，认证规则都必须一致才行.
    private final Mono<ProtocolSupport> supportMono;

    //注销监听器
    private Disposable disposable;

    //设备网关消息处理工具类
    private final DeviceGatewayHelper helper;

    public MqttServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   DeviceSessionManager sessionManager,
                                   MqttServer mqttServer,
                                   DecodedClientMessageHandler messageHandler,
                                   Mono<ProtocolSupport> customProtocol) {
        super(id);
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.mqttServer = mqttServer;
        this.messageHandler = messageHandler;
        this.supportMono = customProtocol;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = mqttServer
            //监听连接,mqtt网关可以复用网络网络组件,多个网关不能收到相同的连接信息
            .handleConnection("device-gateway")
            .filter(conn -> {
                //暂停或者已停止时.
                if (!isStarted()) {
                    //直接响应SERVER_UNAVAILABLE
                    conn.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    monitor.rejected();
                }
                return true;
            })
            //处理mqtt连接请求
            .flatMap(connection -> this
                         .handleConnection(connection)
                         .flatMap(tuple3 -> handleAuthResponse(tuple3.getT1(), tuple3.getT2(), tuple3.getT3()))
                         .flatMap(tp -> handleAcceptedMqttConnection(tp.getT1(), tp.getT2(), tp.getT3()))
                         .onErrorResume(err -> {
                             log.error(err.getMessage(), err);
                             return Mono.empty();
                         }),
                     Integer.MAX_VALUE)
            .subscribe();

    }

    //处理连接，并进行认证
    private Mono<Tuple3<DeviceOperator, AuthenticationResponse, MqttConnection>> handleConnection(MqttConnection connection) {

        return Mono
            .justOrEmpty(connection.getAuth())
            .flatMap(auth -> {
                MqttAuthenticationRequest request = new MqttAuthenticationRequest(connection.getClientId(),
                                                                                  auth.getUsername(),
                                                                                  auth.getPassword(),
                                                                                  getTransport());
                return supportMono
                    //使用自定义协议来认证
                    .map(support -> support.authenticate(request, registry))
                    //没有指定自定义协议,则使用clientId对应的设备进行认证.
                    .defaultIfEmpty(Mono.defer(() -> registry
                        .getDevice(connection.getClientId())
                        .flatMap(device -> device.authenticate(request))))
                    .flatMap(Function.identity())
                    //如果认证结果返回空,说明协议没有设置认证,或者认证返回不对,默认返回BAD_USER_NAME_OR_PASSWORD,防止由于协议编写不当导致mqtt任意访问的安全问题.
                    .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)));
            })
            .flatMap(resp -> {
                //认证响应可以自定义设备ID,如果没有则使用mqtt的clientId
                String deviceId = StringUtils.isEmpty(resp.getDeviceId()) ? connection.getClientId() : resp.getDeviceId();
                //认证返回了新的设备ID,则使用新的设备
                return registry
                    .getDevice(deviceId)
                    .map(operator -> Tuples.of(operator, resp, connection))
                    //设备不存在,应答IDENTIFIER_REJECTED
                    .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)))
                    ;
            })
            .as(MonoTracer
                    .create(SpanName.auth(connection.getClientId()),
                            (span, tp3) -> {
                                AuthenticationResponse response = tp3.getT2();
                                if (!response.isSuccess()) {
                                    span.setStatus(StatusCode.ERROR, response.getMessage());
                                }
                            },
                            (span, hasValue) -> {
                                //empty
                                if (!hasValue) {
                                    span.setStatus(StatusCode.ERROR, "device not exists");
                                }
                                InetSocketAddress address = connection.getClientAddress();
                                if (address != null) {
                                    span.setAttribute(SpanKey.address, address.toString());
                                }
                                span.setAttribute(clientId, connection.getClientId());
                            }))
            //设备认证错误,拒绝连接
            .onErrorResume((err) -> Mono.fromRunnable(() -> {
                log.error("MQTT连接认证[{}]失败", connection.getClientId(), err);
                //监控信息
                monitor.rejected();
                //应答SERVER_UNAVAILABLE
                connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
            }))
            ;
    }

    //处理认证结果
    private Mono<Tuple3<MqttConnection, DeviceOperator, MqttConnectionSession>> handleAuthResponse(DeviceOperator device,
                                                                                                   AuthenticationResponse resp,
                                                                                                   MqttConnection connection) {
        return Mono
            .defer(() -> {
                String deviceId = device.getDeviceId();
                //认证通过
                if (resp.isSuccess()) {
                    //监听断开连接
                    connection.onClose(conn -> {
                        counter.decrement();
                        //监控信息
                        monitor.disconnected();
                        monitor.totalConnection(counter.sum());

                        sessionManager
                            .getSession(deviceId, false)
                            .flatMap(_tmp -> {
                                //只有与创建的会话相同才移除(下线),因为有可能设置了keepOnline,
                                //或者设备通过其他方式注册了会话,这里断开连接不能影响到以上情况.
                                if (_tmp != null && _tmp.isWrapFrom(MqttConnectionSession.class) && !(_tmp instanceof KeepOnlineSession)) {
                                    MqttConnectionSession connectionSession = _tmp.unwrap(MqttConnectionSession.class);
                                    if (connectionSession.getConnection() == conn) {
                                        return sessionManager.remove(deviceId, true);
                                    }
                                }
                                return Mono.empty();
                            })
                            .subscribe();
                    });

                    counter.increment();
                    return sessionManager
                        .compute(deviceId, old -> {
                            MqttConnectionSession newSession = new MqttConnectionSession(deviceId, device, getTransport(), connection, monitor);
                            return old
                                .<DeviceSession>map(session -> {
                                    if (session instanceof KeepOnlineSession) {
                                        //KeepOnlineSession 则依旧保持keepOnline
                                        return new KeepOnlineSession(newSession, session.getKeepAliveTimeout());
                                    }
                                    return newSession;
                                })
                                .defaultIfEmpty(newSession);
                        })
                        .mapNotNull(session->{
                            try {
                                return Tuples.of(connection.accept(), device, session.unwrap(MqttConnectionSession.class));
                            } catch (IllegalStateException ignore) {
                                //忽略错误,偶尔可能会出现网络异常,导致accept时,连接已经中断.还有其他更好的处理方式?
                                return null;
                            }
                        })
                        .doOnNext(o -> {
                            //监控信息
                            monitor.connected();
                            monitor.totalConnection(counter.sum());
                        })
                        //会话empty说明注册会话失败?
                        .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)));
                } else {
                    //认证失败返回 0x04 BAD_USER_NAME_OR_PASSWORD
                    connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                    monitor.rejected();
                    log.warn("MQTT客户端认证[{}]失败:{}", deviceId, resp.getMessage());
                }
                return Mono.empty();
            })
            .onErrorResume(error -> Mono.fromRunnable(() -> {
                log.error(error.getMessage(), error);
                monitor.rejected();
                //发生错误时应答 SERVER_UNAVAILABLE
                connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
            }))
            ;
    }

    //处理已经建立连接的MQTT连接
    private Mono<Void> handleAcceptedMqttConnection(MqttConnection connection,
                                                    DeviceOperator operator,
                                                    MqttConnectionSession session) {
        return Flux
            .usingWhen(Mono.just(connection),
                       MqttConnection::handleMessage,
                       MqttConnection::close)
            //网关暂停或者已停止时,则不处理消息
            .filter(pb -> isStarted())
            .publishOn(Schedulers.parallel())
            //解码收到的mqtt报文
            .concatMap(publishing -> this
                .decodeAndHandleMessage(operator, session, publishing, connection)
                .as(MonoTracer
                        .create(SpanName.upstream(connection.getClientId()),
                                (span) -> span.setAttribute(SpanKey.message, publishing.print())))
            )
            //合并遗言消息
            .mergeWith(
                Mono.justOrEmpty(connection.getWillMessage())
                    //解码遗言消息
                    .flatMap(mqttMessage -> this.decodeAndHandleMessage(operator, session, mqttMessage, connection))
            )
            .then();
    }

    //解码消息并处理
    private Mono<Void> decodeAndHandleMessage(DeviceOperator operator,
                                              MqttConnectionSession session,
                                              MqttMessage message,
                                              MqttConnection connection) {
        monitor.receivedMessage();

        return operator
            .getProtocol()
            .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
            //解码
            .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(
                session, message, registry,msg->handleMessage(operator,msg,connection).then())))
            .cast(DeviceMessage.class)
            .concatMap(msg -> {
                //回填deviceId,有的场景协议包不能或者没有解析出deviceId,则直接使用连接对应的设备id进行填充.
                if (!StringUtils.hasText(msg.getDeviceId())) {
                    msg.thingId(DeviceThingType.device, operator.getDeviceId());
                }
                return this.handleMessage(operator, msg, connection);
            })
            .doOnComplete(() -> {
                if (message instanceof MqttPublishing) {
                    ((MqttPublishing) message).acknowledge();
                }
            })
            .as(FluxTracer
                    .create(SpanName.decode(operator.getDeviceId()),
                            (span, msg) -> span.setAttribute(SpanKey.message, toJsonString(msg.toJson()))))
            //发生错误不中断流
            .onErrorResume((err) -> {
                log.error("handle mqtt message [{}] error:{}", operator.getDeviceId(), message, err);
                return Mono.empty();
            })
            .then()
            ;
    }

    @SneakyThrows
    private String toJsonString(Object data){
        return ObjectMappers.JSON_MAPPER.writeValueAsString(data);
    }

    private Mono<DeviceMessage> handleMessage(DeviceOperator mainDevice,
                                              DeviceMessage message,
                                              MqttConnection connection) {
        //连接已经断开,直接处理消息,不再处理会话
        //有的场景下，设备发送了消息,立即就断开了连接,这是会话已经失效了,如果还继续创建会话的话会出现多次上线的问题.
        if (!connection.isAlive()) {
            return messageHandler
                .handleMessage(mainDevice, message)
                .thenReturn(message);
        }
        //统一处理解码后的设备消息
        return helper.handleDeviceMessage(message,
                                          device -> new MqttConnectionSession(device.getDeviceId(),
                                                                              device,
                                                                              getTransport(),
                                                                              connection,
                                                                              monitor),
                                          session -> {

                                          },
                                          () -> {
                                              log.warn("无法从MQTT[{}]消息中获取设备信息:{}", connection.getClientId(), message);
                                          })
                     .thenReturn(message);
    }

    @Override
    protected Mono<Void> doStartup() {
        doStart();
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doShutdown() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        return Mono.empty();
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }
}
