package org.jetlinks.community.device.message;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.firmware.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 将设备消息连接到消息网关
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class DeviceMessageConnector{
    //将设备注册中心到配置追加到消息header中,下游订阅者可直接使用.
    private final String[] appendConfigHeader = {"productId", "deviceName"};

    //设备注册中心
    private final DeviceRegistry registry;

    private final EventBus eventBus;

    private final static BiConsumer<Throwable, Object> doOnError = (error, val) -> log.error(error.getMessage(), error);

    private final Function<DeviceOperator, Mono<Values>> configGetter;

    private final static Values emptyValues = Values.of(Collections.emptyMap());

    public DeviceMessageConnector(EventBus eventBus,
                                  DeviceRegistry registry) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.configGetter = operator -> operator.getSelfConfigs(appendConfigHeader);
    }

    public Mono<Void> onMessage(Message message) {
        if (null == message) {
            return Mono.empty();
        }
        return this.getTopic(message)
            .flatMap(topic -> eventBus.publish(topic, message).then())
            .onErrorContinue(doOnError)
            .then();
    }

    public Mono<String> getTopic(Message message) {
        if (message instanceof DeviceMessage) {
            DeviceMessage deviceMessage = ((DeviceMessage) message);
            String deviceId = deviceMessage.getDeviceId();
            if (deviceId == null) {
                log.warn("无法从消息中获取设备ID:{}", deviceMessage);
                return Mono.empty();
            }
            return registry
                .getDevice(deviceId)
                .flatMap(configGetter)
                .defaultIfEmpty(emptyValues)
                .flatMap(configs -> {
                    configs.getAllValues().forEach(deviceMessage::addHeader);
                    String productId = deviceMessage.getHeader("productId").map(String::valueOf).orElse("null");
                    String topic = createDeviceMessageTopic(productId, deviceId, deviceMessage);

                    if (message instanceof ChildDeviceMessage) { //子设备消息
                        return onMessage(((ChildDeviceMessage) message).getChildDeviceMessage())
                            .thenReturn(topic);
                    } else if (message instanceof ChildDeviceMessageReply) { //子设备消息
                        return onMessage(((ChildDeviceMessageReply) message).getChildDeviceMessage())
                            .thenReturn(topic);
                    }
                    return Mono.just(topic);
                });

        }
        return Mono.just("/device/unknown/message/unknown");
    }

    public static String createDeviceMessageTopic(String productId, String deviceId, DeviceMessage message) {
        StringBuilder builder = new StringBuilder(64)
            .append("/device/")
            .append(productId)
            .append("/")
            .append(deviceId);

        appendDeviceMessageTopic(message, builder);
        return builder.toString();
    }

    public static void appendDeviceMessageTopic(Message message, StringBuilder builder) {
        if (message instanceof EventMessage) {   //事件
            EventMessage event = ((EventMessage) message);
            builder.append("/message/event/").append(event.getEvent());
        } else if (message instanceof ReportPropertyMessage) {   //上报属性
            builder.append("/message/property/report");
        } else if (message instanceof DeviceOnlineMessage) {   //设备上线
            builder.append("/online");
        } else if (message instanceof DeviceOfflineMessage) {   //设备离线
            builder.append("/offline");
        } else if (message instanceof ChildDeviceMessage) { //子设备消息
            Message msg = ((ChildDeviceMessage) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                builder.append("/message/children/")
                    .append(((DeviceMessage) msg).getDeviceId());
                appendDeviceMessageTopic(msg, builder);
            } else {
                builder.append("/message/children");
                appendDeviceMessageTopic(message, builder);
            }
        } else if (message instanceof ChildDeviceMessageReply) { //子设备消息
            Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                builder.append("/message/children/reply/")
                    .append(((DeviceMessage) msg).getDeviceId());
                appendDeviceMessageTopic(msg, builder);
            } else {
                builder.append("/message/children/reply");
                appendDeviceMessageTopic(message, builder);
            }
        } else if (message instanceof ReadPropertyMessage) { //读取属性
            builder.append("/message/send/property/read");
        } else if (message instanceof WritePropertyMessage) { //修改属性
            builder.append("/message/send/property/write");
        } else if (message instanceof FunctionInvokeMessage) { //调用功能
            builder.append("/message/send/function");
        } else if (message instanceof ReadPropertyMessageReply) { //读取属性回复
            builder.append("/message/property/read/reply");
        } else if (message instanceof WritePropertyMessageReply) { //修改属性回复
            builder.append("/message/property/write/reply");
        } else if (message instanceof FunctionInvokeMessageReply) { //调用功能回复
            builder.append("/message/function/reply");
        } else if (message instanceof DeviceRegisterMessage) { //注册
            builder.append("/register");
        } else if (message instanceof DeviceUnRegisterMessage) { //注销
            builder.append("/unregister");
        } else if (message instanceof RequestFirmwareMessage) { //拉取固件请求 since 1.3
            builder.append("/firmware/pull");
        } else if (message instanceof RequestFirmwareMessageReply) { //拉取固件响应 since 1.3
            builder.append("/firmware/pull/reply");
        } else if (message instanceof ReportFirmwareMessage) { //上报固件信息 since 1.3
            builder.append("/firmware/report");
        } else if (message instanceof UpgradeFirmwareProgressMessage) { //上报固件更新进度 since 1.3
            builder.append("/firmware/progress");
        } else if (message instanceof UpgradeFirmwareMessage) { //推送固件更新 since 1.3
            builder.append("/firmware/push");
        } else if (message instanceof UpgradeFirmwareMessageReply) { //推送固件更新回复 since 1.3
            builder.append("/firmware/push/reply");
        } else if (message instanceof DirectDeviceMessage) { //透传消息 since 1.4
            builder.append("/message/direct");
        } else {
            builder.append("/message/").append(message.getMessageType().name().toLowerCase());
        }
    }
}
