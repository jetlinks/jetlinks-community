package org.jetlinks.community.network.utils;

import lombok.AllArgsConstructor;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.state.DeviceStateCheckMessage;
import org.jetlinks.core.message.state.DeviceStateCheckMessageReply;
import org.jetlinks.core.server.session.ChildrenDeviceSession;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.KeepOnlineSession;
import org.jetlinks.core.server.session.LostDeviceSession;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 设备网关处理工具
 * <p>
 * 封装常用的设备消息处理操作
 * </p>
 *
 * @author zhouhao
 */
@AllArgsConstructor
public class DeviceGatewayHelper {

    private final DeviceRegistry registry;
    private final DeviceSessionManager sessionManager;
    private final DecodedClientMessageHandler messageHandler;


    public static Consumer<DeviceSession> applySessionKeepaliveTimeout(DeviceMessage msg, Supplier<Duration> timeoutSupplier) {
        return session -> {
            //从消息头里获取keepOnlineTimeoutSeconds来设置会话有效期
            Duration timeout = msg
                .getHeader(Headers.keepOnlineTimeoutSeconds)
                .map(Duration::ofSeconds)
                .orElseGet(timeoutSupplier);
            if (null != timeout) {
                session.setKeepAliveTimeout(timeout);
            }
        };
    }

    /**
     * 处理设备消息
     *
     * @param message                设备消息
     * @param sessionBuilder         会话构造器,在会话不存在时,创建会话
     * @param sessionConsumer        会话自定义回调,处理会话时用来自定义会话,比如重置连接信息
     * @param deviceNotFoundCallback 设备不存在的监听器回调
     * @return 设备操作接口
     */
    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, DeviceSession> sessionBuilder,
                                                    Consumer<DeviceSession> sessionConsumer,
                                                    Runnable deviceNotFoundCallback) {

        return handleDeviceMessage(message, sessionBuilder, sessionConsumer, () -> Mono.fromRunnable(deviceNotFoundCallback));
    }

    protected Mono<Void> handleChildrenDeviceMessage(String deviceId, DeviceMessage children) {
        //设备状态检查,断开设备连接的消息都忽略
        //这些消息属于状态管理,通常是用来自定义子设备状态的,所以这些消息都忽略处理会话
        if (deviceId == null
            || children instanceof DeviceStateCheckMessage
            || children instanceof DeviceStateCheckMessageReply
            || children instanceof DisconnectDeviceMessage
            || children instanceof DisconnectDeviceMessageReply) {
            return Mono.empty();
        }
        //子设备回复失败的也忽略
        if (children instanceof DeviceMessageReply) {
            DeviceMessageReply reply = ((DeviceMessageReply) children);
            if (!reply.isSuccess()) {
                return Mono.empty();
            }
        }
        String childrenId = children.getDeviceId();

        //子设备离线或者注销
        if (children instanceof DeviceOfflineMessage || children instanceof DeviceUnRegisterMessage) {
            //注销会话,这里子设备可能会收到多次离线消息
            //注销会话一次离线,消息网关转发子设备消息一次
            return sessionManager
                .remove(childrenId, children.getHeaderOrDefault(Headers.clearAllSession))
                .doOnNext(total -> {
                    if (total > 0) {
                        children.addHeader(Headers.ignore, true);
                    }
                })
                .then();
        } else {
            //子设备上线
            if (children instanceof DeviceOnlineMessage) {
                children.addHeader(Headers.ignore, true);
            }
            Mono<DeviceSession> registerSession = sessionManager
                .getSession(deviceId)
                .flatMap(parentSession -> sessionManager
                    .compute(childrenId, old -> old
                        .switchIfEmpty(Mono.defer(() -> registry
                            .getDevice(childrenId)
                            .map(child -> new ChildrenDeviceSession(childrenId, parentSession, child)))))
                )
                .doOnNext(session -> {
                    session.keepAlive();
                    applySessionKeepaliveTimeout(children, () -> null)
                        .accept(session);
                });


            //子设备注册
            if (isDoRegister(children)) {
                return Mono
                    //延迟2秒，因为自动注册是异步的,收到消息后并不能保证马上可以注册成功.
                    .delay(Duration.ofSeconds(2))
                    .then(registry
                              .getDevice(children.getDeviceId())
                              .flatMap(device -> device
                                  //没有配置状态自管理才自动上线
                                  .getSelfConfig(DeviceConfigKey.selfManageState)
                                  .defaultIfEmpty(false)
                                  .filter(Boolean.FALSE::equals))
                              .flatMap(ignore -> registerSession))
                    .then();
            }
            return registerSession.then();
        }
    }

    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, Mono<DeviceSession>> sessionBuilder,
                                                    Function<DeviceSession, Mono<Void>> sessionConsumer,
                                                    Supplier<Mono<DeviceOperator>> deviceNotFoundCallback) {
        String deviceId = message.getDeviceId();
        if (StringUtils.isEmpty(deviceId)) {
            return Mono.empty();
        }
        Mono<Void> then = Mono.empty();
        boolean doHandle = true;
        //子设备消息
        if (message instanceof ChildDeviceMessage) {
            DeviceMessage childrenMessage = (DeviceMessage) ((ChildDeviceMessage) message).getChildDeviceMessage();
            then = handleChildrenDeviceMessage(deviceId, childrenMessage);
        }
        //子设备消息回复
        else if (message instanceof ChildDeviceMessageReply) {
            DeviceMessage childrenMessage = (DeviceMessage) ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            then = handleChildrenDeviceMessage(deviceId, childrenMessage);
        }
        //设备离线消息
        else if (message instanceof DeviceOfflineMessage) {
            return sessionManager
                .remove(deviceId, message.getHeaderOrDefault(Headers.clearAllSession))
                .flatMap(l -> {
                    if (l == 0) {
                        return registry
                            .getDevice(deviceId)
                            .flatMap(device -> messageHandler.handleMessage(device, message));
                    }
                    return Mono.empty();
                })
                .then(
                    registry.getDevice(deviceId)
                )
                .contextWrite(Context.of(DeviceMessage.class, message));
        }
        //设备在线消息
        else if (message instanceof DeviceOnlineMessage) {

            doHandle = false;
        }

        boolean fHandle = doHandle;
        return sessionManager
            .compute(deviceId, (old) -> old
                .map(session -> {
                    //会话已存在
                    Mono<Void> after = Mono.empty();
                    //没有忽略会话
                    if (!message.getHeader(Headers.ignoreSession).orElse(false)) {
                        //消息中指定保存在线
                        if (message.getHeader(Headers.keepOnline).orElse(false)
                            && !(session instanceof KeepOnlineSession)) {
                            Duration timeout = message
                                .getHeader(Headers.keepOnlineTimeoutSeconds)
                                .map(Duration::ofSeconds)
                                .orElse(Duration.ofHours(1));
                            //替换session
                            session = new KeepOnlineSession(session, timeout);
                        }
                        //KeepOnline的连接丢失时，重新创建会话，并替换丢失的会话。
                        if (session.isWrapFrom(KeepOnlineSession.class) && session.isWrapFrom(LostDeviceSession.class)) {
                            after = sessionBuilder
                                .apply(session.getOperator())
                                .doOnNext(session.unwrap(KeepOnlineSession.class)::replaceWith)
                                .then();
                        }
                        after = after.then(
                            sessionConsumer.apply(session)
                        );

                    }
                    session.keepAlive();
                    if (fHandle) {
                        //处理消息
                        return messageHandler
                            .handleMessage(session.getOperator(), message)
                            .then(after)
                            .thenReturn(session);
                    }
                    return after
                        .thenReturn(session);
                })
                .defaultIfEmpty(Mono.defer(() -> registry
                    .getDevice(deviceId)
                    .switchIfEmpty(Mono.defer(() -> {
                        //设备注册
                        if (isDoRegister(message)) {
                            return messageHandler
                                .handleMessage(null, message)
                                //延迟2秒后尝试重新获取设备并上线
                                .then(Mono.delay(Duration.ofSeconds(2)))
                                .then(registry.getDevice(deviceId));
                        }
                        if (deviceNotFoundCallback != null) {
                            return deviceNotFoundCallback.get();
                        }
                        return Mono.empty();
                    }))
                    .flatMap(device -> {
                        //忽略会话管理,比如一个设备存在多种接入方式时,其中一种接入方式收到的消息设置忽略会话来防止会话冲突
                        if (message.getHeader(Headers.ignoreSession).orElse(false)) {
                            if (!isDoRegister(message)) {
                                return messageHandler
                                    .handleMessage(device, message)
                                    .then(Mono.empty());
                            }
                            return Mono.empty();
                        }
                        return sessionBuilder
                            .apply(device)
                            .flatMap(newSession -> {
                                //保持会话，在低功率设备上,可能无法保持长连接.
                                if (message.getHeader(Headers.keepOnline).orElse(false)) {
                                    int timeout = message.getHeaderOrDefault(Headers.keepOnlineTimeoutSeconds);
                                    newSession = new KeepOnlineSession(newSession, Duration.ofSeconds(timeout));
                                }
                                //执行自定义会话回调
                                sessionConsumer.apply(newSession);
                                //保活
                                newSession.keepAlive();
                                if (!(message instanceof DeviceRegisterMessage) &&
                                    !(message instanceof DeviceOnlineMessage)) {
                                    return
                                        sessionConsumer
                                            .apply(newSession)
                                            .then(
                                                messageHandler
                                                    .handleMessage(device, message)
                                            )
                                            .thenReturn(newSession);
                                } else {
                                    return sessionConsumer
                                        .apply(newSession)
                                        .thenReturn(newSession);
                                }
                            });
                    })))
                .flatMap(Function.identity()))
            .then(then)
            .then(registry.getDevice(deviceId))
            .contextWrite(Context.of(DeviceMessage.class,message));


    }


    /**
     * 处理设备消息
     *
     * @param message                设备消息
     * @param sessionBuilder         会话构造器,在会话不存在时,创建会话
     * @param sessionConsumer        会话自定义回调,处理会话时用来自定义会话,比如重置连接信息
     * @param deviceNotFoundCallback 设备不存在的监听器回调
     * @return 设备操作接口
     */
    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, DeviceSession> sessionBuilder,
                                                    Consumer<DeviceSession> sessionConsumer,
                                                    Supplier<Mono<DeviceOperator>> deviceNotFoundCallback) {
        return this
            .handleDeviceMessage(
                message,
                device -> Mono.justOrEmpty(sessionBuilder.apply(device)),
                session -> {
                    sessionConsumer.accept(session);
                    return Mono.empty();
                },
                deviceNotFoundCallback
            );

    }

    private boolean isDoRegister(DeviceMessage message) {
        return message instanceof DeviceRegisterMessage
            && message.getHeader(PropertyConstants.deviceName).isPresent()
            && message.getHeader(PropertyConstants.productId).isPresent();
    }



}
