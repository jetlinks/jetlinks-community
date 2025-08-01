/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.jetlinks.core.message.Headers.ignoreIfOffline;

/**
 * 设备网关消息处理,会话管理工具类,用于统一封装对设备消息和会话的处理逻辑
 *
 * @author zhouhao
 * @see DeviceRegistry
 * @see DecodedClientMessageHandler
 * @since 1.5
 */
@AllArgsConstructor
@Getter
public class DeviceGatewayHelper {

    private final DeviceRegistry registry;
    private final DeviceSessionManager sessionManager;
    private final DecodedClientMessageHandler messageHandler;

    public static Consumer<DeviceSession> applySessionKeepaliveTimeout(DeviceMessage msg, Supplier<Duration> timeoutSupplier) {
        return session -> {
            Integer timeout = msg.getHeaderOrElse(Headers.keepOnlineTimeoutSeconds, () -> null);
            if (null != timeout) {
                session.setKeepAliveTimeout(Duration.ofSeconds(timeout));
            } else {
                Duration defaultTimeout = timeoutSupplier.get();
                if (null != defaultTimeout) {
                    session.setKeepAliveTimeout(defaultTimeout);
                }
            }
        };
    }

    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, DeviceSession> sessionBuilder) {

        return handleDeviceMessage(message,
                                   sessionBuilder,
                                   (ignore) -> {
                                   },
                                   () -> {
                                   });
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

    private void handleChildrenDeviceMessage(String deviceId, DeviceMessage children, HandlerContext ctx) {
        //设备状态检查,断开设备连接的消息都忽略
        //这些消息属于状态管理,通常是用来自定义子设备状态的,所以这些消息都忽略处理会话
        if (deviceId == null
            || children instanceof DeviceStateCheckMessage
            || children instanceof DeviceStateCheckMessageReply
            || children instanceof DisconnectDeviceMessage
            || children instanceof DisconnectDeviceMessageReply
            || children.getHeaderOrDefault(Headers.ignoreSession)) {
            return;
        }
        //子设备回复失败的也忽略
        if (children instanceof DeviceMessageReply) {
            DeviceMessageReply reply = ((DeviceMessageReply) children);
            if (!reply.isSuccess()) {
                return;
            }
        }
        String childrenId = children.getDeviceId();

        //子设备离线或者注销
        if (children instanceof DeviceOfflineMessage || children instanceof DeviceUnRegisterMessage) {
            //注销会话,这里子设备可能会收到多次离线消息
            //注销会话一次离线,消息网关转发子设备消息一次
            //先执行移除子设备会话,防止header设置失败
            ctx.before(
                sessionManager
                    .remove(childrenId, removeSessionOnlyLocal(children))
                    .doOnNext(total -> {
                        //移除了会话会触发离线消息,忽略掉本次的消息.
                        if (total > 0 && children instanceof DeviceOfflineMessage) {
                            children.addHeaderIfAbsent(Headers.ignore, true);
                        }
                        //没有会话被移除(已经离线),但是手动指定了忽略离线消息.
                        if (total == 0 && children.getHeaderOrDefault(ignoreIfOffline)) {
                            children.addHeader(Headers.ignore, true);
                        }
                    })
                    .then()
                    .contextWrite(Context.of(DeviceMessage.class, children))
            );
        } else {
            //子设备上线
            if (children instanceof DeviceOnlineMessage) {
                //没有标记force,则忽略上线消息,避免产生重复的上线消息.
                if (!children.getHeader(Headers.force).orElse(false)) {
                    children.addHeaderIfAbsent(Headers.ignore, true);
                }
            }
            //子设备会话处理
            Mono<DeviceSession> sessionHandler = children.getHeaderOrDefault(Headers.ignoreSession)
                ? Mono.empty()
                : sessionManager
                .getSession(deviceId)
                .flatMap(parentSession -> this
                    .createOrUpdateSession(
                        childrenId,
                        children,
                        child -> {
                            //新创建了的会话?
                            return Mono.just(new ChildrenDeviceSession(childrenId, parentSession, child));
                        },
                        Mono::empty)
                    .doOnNext(session -> {
                        if (session.isWrapFrom(ChildrenDeviceSession.class)) {
                            ChildrenDeviceSession childrenSession = session.unwrap(ChildrenDeviceSession.class);
                            //网关发生变化,替换新的上级会话
                            if (!Objects.equals(deviceId, childrenSession.getParent().getDeviceId())) {
                                childrenSession.replaceWith(parentSession);
                            }
                        }
                    }))
                   .contextWrite(Context.of(DeviceMessage.class, children));


            //子设备注册
            if (isDoRegister(children)) {
                ctx.after(
                    this
                        .getDeviceForRegister(children.getDeviceId())
                        .flatMap(device -> device
                            //没有配置状态自管理才自动上线
                            .getSelfConfig(DeviceConfigKey.selfManageState)
                            .defaultIfEmpty(false)
                            .filter(Boolean.FALSE::equals))
                        .flatMap(ignore -> sessionHandler)
                        .then()
                );
            } else {
                ctx.after(sessionHandler.then());
            }

        }
    }

    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, Mono<DeviceSession>> sessionBuilder,
                                                    Function<DeviceSession, Mono<Void>> sessionConsumer,
                                                    Supplier<Mono<DeviceOperator>> deviceNotFoundCallback) {
        String deviceId = message.getDeviceId();
        if (!StringUtils.hasText(deviceId)) {
            return Mono.empty();
        }
        HandlerContext ctx = new HandlerContext();

        boolean doHandle = true;
        Context context = Context.of(DeviceMessage.class, message);
        //子设备消息
        if (message instanceof ChildDeviceMessage) {
            DeviceMessage childrenMessage = (DeviceMessage) ((ChildDeviceMessage) message).getChildDeviceMessage();
            handleChildrenDeviceMessage(deviceId, childrenMessage, ctx);
        }
        //子设备消息回复
        else if (message instanceof ChildDeviceMessageReply) {
            DeviceMessage childrenMessage = (DeviceMessage) ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            handleChildrenDeviceMessage(deviceId, childrenMessage, ctx);
        }
        //设备离线消息
        else if (message instanceof DeviceOfflineMessage) {
            return sessionManager
                .remove(deviceId, removeSessionOnlyLocal(message))
                .flatMap(l -> {
                    if (l == 0 && !message.getHeaderOrDefault(ignoreIfOffline)) {
                        return registry
                            .getDevice(deviceId)
                            .flatMap(device -> handleMessage(device, message));
                    }
                    return Mono.empty();
                })
                .then(registry.getDevice(deviceId))
                .contextWrite(context);
        }
        //设备上线消息,不发送到messageHandler,防止设备上线存在重复消息
        else if (message instanceof DeviceOnlineMessage) {
            doHandle = message
                .getHeader(Headers.force)
                .orElse(false);
        }

        //忽略会话管理,比如一个设备存在多种接入方式时,其中一种接入方式收到的消息设置忽略会话来防止会话冲突
        if (message.getHeaderOrDefault(Headers.ignoreSession)) {
//            if (!isDoRegister(message)) {
            return ctx
                .execute(handleMessage(null, message))
                .then(registry.getDevice(deviceId))
                .contextWrite(context);
//            }
//            return ctx
//                .execute(Mono.empty())
//                .then(registry.getDevice(deviceId))
//                .contextWrite(context);
        }

        if (doHandle) {
            ctx.after(handleMessage(null, message));
        }

        return ctx
            .execute(
                this
                    .createOrUpdateSession(deviceId, message, sessionBuilder, deviceNotFoundCallback)
                    .flatMap(sessionConsumer)
            )
            .then(registry.getDevice(deviceId))
            .contextWrite(context);
//        return this
//            .createOrUpdateSession(deviceId, message, sessionBuilder, deviceNotFoundCallback)
//            .flatMap(sessionConsumer)
//            .then(then)
//            .contextWrite(context);

    }

    private Mono<Void> handleMessage(DeviceOperator device, Message message) {
        return messageHandler
            .handleMessage(device, message)
            //转换为empty,减少触发discard
            .flatMap(ignore -> Mono.empty());
    }

    private Mono<DeviceSession> createOrUpdateSession(String deviceId,
                                                      DeviceMessage message,
                                                      Function<DeviceOperator, Mono<DeviceSession>> sessionBuilder,
                                                      Supplier<Mono<DeviceOperator>> deviceNotFoundCallback) {
        return sessionManager
            .getSession(deviceId, false)
            .filterWhen(DeviceSession::isAliveAsync)
            .map(old -> {
                //需要更新会话时才进行更新
                if (needUpdateSession(old, message)) {
                    return sessionManager
                        .compute(deviceId, null, session -> updateSession(session, message, sessionBuilder));
                }
                applySessionKeepaliveTimeout(message, old);
                old.keepAlive();
                return Mono.just(old);
            })
            //会话不存在则尝试创建或者更新
            .defaultIfEmpty(Mono.defer(() -> sessionManager
                .compute(deviceId,
                         createNewSession(
                             deviceId,
                             message,
                             sessionBuilder,
                             () -> {
                                 //设备注册
                                 if (isDoRegister(message)) {
                                     return this
                                         .handleMessage(null, message)
                                         //延迟2秒后尝试重新获取设备并上线
                                         .then(Mono.delay(Duration.ofSeconds(2)))
                                         .then(registry.getDevice(deviceId));
                                 }
                                 if (deviceNotFoundCallback != null) {
                                     return deviceNotFoundCallback.get();
                                 }
                                 return Mono.empty();
                             }),
                         session -> updateSession(session, message, sessionBuilder))))
            .flatMap(Function.identity())
            .map(session -> handleSession(message, session));
    }

    private Mono<DeviceOperator> getDeviceForRegister(String deviceId) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.defer(() -> Mono
                //延迟2秒，因为自动注册是异步的,收到消息后并不能保证马上可以注册成功.
                .delay(Duration.ofSeconds(2))
                .then(registry.getDevice(deviceId))));
    }

    private Mono<DeviceSession> createNewSession(String deviceId,
                                                 DeviceMessage message,
                                                 Function<DeviceOperator, Mono<DeviceSession>> sessionBuilder,
                                                 Supplier<Mono<DeviceOperator>> deviceNotFoundCallback) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(Mono.defer(deviceNotFoundCallback))
            .flatMap(device -> sessionBuilder
                .apply(device)
                .map(newSession -> {
                    //保持在线，在低功率设备上,可能无法保持长连接,通过keepOnline的header来标识让设备保持在线
                    if (message.getHeader(Headers.keepOnline).orElse(false)) {
                        int timeout = message.getHeaderOrDefault(Headers.keepOnlineTimeoutSeconds);
                        newSession = new KeepOnlineSession(newSession, Duration.ofSeconds(timeout));
                    }
                    return newSession;
                }));
    }

    private Mono<DeviceSession> updateSession(DeviceSession session,
                                              DeviceMessage message,
                                              Function<DeviceOperator, Mono<DeviceSession>> sessionBuilder) {

        return session
            .isAliveAsync()
            .flatMap(alive -> {
                //设备会话存活才更新
                if (alive) {
                    return updateSession0(session, message, sessionBuilder);
                }
                //创建新的会话
                return createNewSession(message.getDeviceId(), message, sessionBuilder, Mono::empty);
            });
    }

    private Mono<DeviceSession> updateSession0(DeviceSession session,
                                               DeviceMessage message,
                                               Function<DeviceOperator, Mono<DeviceSession>> sessionBuilder) {
        Mono<DeviceSession> after = null;
        //消息中指定保持在线,并且之前的会话不是保持在线,则需要替换之前的会话
        if (isNewKeeOnline(session, message)) {
            Integer timeoutSeconds = message.getHeaderOrDefault(Headers.keepOnlineTimeoutSeconds);
            //替换session
            session = new KeepOnlineSession(session, Duration.ofSeconds(timeoutSeconds));
        }
        //KeepOnline的连接丢失时(服务重启等操作),设备上线后替换丢失的会话,让其能恢复下行能力。
        if (isKeeOnlineLost(session)) {
            Integer timeoutSeconds = message.getHeaderOrDefault(Headers.keepOnlineTimeoutSeconds);
            after = sessionBuilder
                .apply(session.getOperator())
                .map(newSession -> new KeepOnlineSession(newSession, Duration.ofSeconds(timeoutSeconds)));
        }
        applySessionKeepaliveTimeout(message, session);
        session.keepAlive();
        return after == null
            ? Mono.just(session)
            : after;
    }

    private DeviceSession handleSession(DeviceMessage message, DeviceSession session) {
        //尝试设置ignoreParent
        if (session.isWrapFrom(KeepOnlineSession.class)) {
            message
                .getHeader(Headers.keepOnlineIgnoreParent)
                .ifPresent(session.unwrap(KeepOnlineSession.class)::setIgnoreParent);
        }

        return session;
    }

    private static void applySessionKeepaliveTimeout(DeviceMessage msg, DeviceSession session) {
        Integer timeout = msg.getHeaderOrElse(Headers.keepOnlineTimeoutSeconds, () -> null);
        if (null != timeout) {
            session.setKeepAliveTimeout(Duration.ofSeconds(timeout));
        }
    }

    //是否只移除当前节点中的会话,默认false,表示下行则移除整个集群的会话.
    //设置addHeader("clearAllSession",false); 表示只移除本地会话
    private boolean removeSessionOnlyLocal(DeviceMessage message) {
        return message
            .getHeader(Headers.clearAllSession)
            .map(val -> !val)
            .orElse(false);
    }

    //判断是否需要更新会话
    private static boolean needUpdateSession(DeviceSession session, DeviceMessage message) {
        return isNewKeeOnline(session, message) || isKeeOnlineLost(session);
    }

    //判断是否为新的保持在线消息
    private static boolean isNewKeeOnline(DeviceSession session, DeviceMessage message) {
        return message.getHeader(Headers.keepOnline).orElse(false) && !(session instanceof KeepOnlineSession);
    }

    //判断保持在线的会话是否以及丢失(服务重启后可能出现)
    private static boolean isKeeOnlineLost(DeviceSession session) {
        if (!session.isWrapFrom(KeepOnlineSession.class)) {
            return false;
        }
        return session.isWrapFrom(LostDeviceSession.class)
            || !session.unwrap(KeepOnlineSession.class).getParent().isAlive();
    }

    //判断是否为设备注册
    private static boolean isDoRegister(DeviceMessage message) {
        return message instanceof DeviceRegisterMessage
            && message.getHeader(PropertyConstants.deviceName).isPresent()
            && message.getHeader(PropertyConstants.productId).isPresent();
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

    /**
     * 校验设备消息的网关ID
     *
     * @param accessId 当前网关ID
     * @param message  设备消息
     * @return 是否一致
     */
    public Mono<Boolean> checkAccessId(@NotNull String accessId, DeviceMessage message) {
        if (message.getHeaderOrDefault(Headers.multiGateway)) {
            return Reactors.ALWAYS_TRUE;
        }
        return registry
            .getDevice(message.getDeviceId())
            .flatMap(operator -> operator.getConfig(PropertyConstants.accessId))
            .map(accessId::equals)
            .defaultIfEmpty(true);
    }


    private static class HandlerContext {
        Mono<Void> before;
        Mono<Void> after;

        public void before(Mono<Void> before) {
            if (this.before == null) {
                this.before = before;
            } else {
                this.before = before.then(this.before);
            }
        }

        public void after(Mono<Void> after) {
            if (this.after == null) {
                this.after = after;
            } else {
                this.after = this.after.then(after);
            }
        }

        public Mono<Void> execute(Mono<Void> executor) {
            Mono<Void> task = executor;
            if (before != null) {
                task = before.then(task);
            }
            if (after != null) {
                task = task.then(after);
            }
            return task;
        }

    }


}
