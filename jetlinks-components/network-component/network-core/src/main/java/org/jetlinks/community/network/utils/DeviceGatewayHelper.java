package org.jetlinks.community.network.utils;

import lombok.AllArgsConstructor;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.core.server.session.KeepOnlineSession;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class DeviceGatewayHelper {

    private final DeviceRegistry registry;
    private final DeviceSessionManager sessionManager;
    private final DecodedClientMessageHandler messageHandler;


    public static Consumer<DeviceSession> applySessionKeepaliveTimeout(DeviceMessage msg, Supplier<Duration> timeoutSupplier) {
        return session -> {
            Duration timeout = msg
                .getHeader(Headers.keepOnlineTimeoutSeconds)
                .map(Duration::ofSeconds)
                .orElseGet(timeoutSupplier);
            if (null != timeout) {
                session.setKeepAliveTimeout(timeout);
            }
        };
    }

    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, DeviceSession> sessionBuilder,
                                                    Consumer<DeviceSession> sessionConsumer,
                                                    Runnable deviceNotFoundListener) {

        return handleDeviceMessage(message, sessionBuilder, sessionConsumer, () -> Mono.fromRunnable(deviceNotFoundListener));
    }

    public Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                    Function<DeviceOperator, DeviceSession> sessionBuilder,
                                                    Consumer<DeviceSession> sessionConsumer,
                                                    Supplier<Mono<DeviceOperator>> deviceNotFoundListener) {

        return handleDeviceMessage(message, sessionBuilder, sessionConsumer, deviceNotFoundListener, false);
    }

    private Mono<DeviceOperator> handleDeviceMessage(DeviceMessage message,
                                                     Function<DeviceOperator, DeviceSession> sessionBuilder,
                                                     Consumer<DeviceSession> sessionConsumer,
                                                     Supplier<Mono<DeviceOperator>> deviceNotFoundListener,
                                                     boolean children) {
        String deviceId = message.getDeviceId();
        Mono<DeviceOperator> then = Mono.empty();
        if (message instanceof ChildDeviceMessage) {
//            then = handleDeviceMessage((DeviceMessage) ((ChildDeviceMessage) message).getChildDeviceMessage(),
//                                       sessionBuilder,
//                                       sessionConsumer,
//                                       deviceNotFoundListener, true);
        } else if (message instanceof ChildDeviceMessageReply) {
//            then = handleDeviceMessage((DeviceMessage) ((ChildDeviceMessageReply) message).getChildDeviceMessage(),
//                                       sessionBuilder,
//                                       sessionConsumer,
//                                       deviceNotFoundListener, true);
        } else if (message instanceof DeviceOnlineMessage) {
            //设备在线消息
            then = registry.getDevice(deviceId);
        } else if (message instanceof DeviceOfflineMessage) {
            //设备离线消息
            DeviceSession session = sessionManager.unregister(deviceId);
            if (null == session) {
                //如果session不存在,则将离线消息转发到
                return registry
                    .getDevice(deviceId)
                    .flatMap(device -> messageHandler
                        .handleMessage(device, message)
                        .thenReturn(device));
            }
            return registry.getDevice(deviceId);
        }
        DeviceSession session = sessionManager.getSession(deviceId);
        //session不存在,可能是同一个连接返回多个设备消息
        if (session == null) {
            return registry
                .getDevice(deviceId)
                .switchIfEmpty(children ? Mono.empty() : Mono.defer(() -> {
                    //设备注册
                    if (message instanceof DeviceRegisterMessage) {
                        if (message.getHeader(PropertyConstants.deviceName).isPresent()
                            && message.getHeader(PropertyConstants.productId).isPresent()) {
                            return messageHandler
                                .handleMessage(null, message)
                                //延迟2秒后尝试重新获取设备并上线
                                .then(Mono.delay(Duration.ofSeconds(2)))
                                .then(registry.getDevice(deviceId));
                        }
                    }
                    if (deviceNotFoundListener != null) {
                        return deviceNotFoundListener.get();
                    }
                    return Mono.empty();
                }))
                .flatMap(device -> {
                    DeviceSession newSession = sessionBuilder.apply(device);
                    //保持会话，在低功率设备上,可能无法保持mqtt长连接.
                    if (message.getHeader(Headers.keepOnline).orElse(false)) {
                        int timeout = message.getHeaderOrDefault(Headers.keepOnlineTimeoutSeconds);
                        newSession = new KeepOnlineSession(newSession, Duration.ofSeconds(timeout));
                    }
                    sessionManager.register(newSession);
                    sessionConsumer.accept(newSession);
                    newSession.keepAlive();
                    if (message instanceof DeviceRegisterMessage) {
                        return messageHandler
                            .handleMessage(device, message)
                            .thenReturn(device);
                    }
                    return Mono.just(device);
                })
                .then(then);
        } else {
            sessionConsumer.accept(session);
            session.keepAlive();
            return then
                .then(messageHandler.handleMessage(session.getOperator(), message))
                .then(registry.getDevice(deviceId));
        }

    }

}
