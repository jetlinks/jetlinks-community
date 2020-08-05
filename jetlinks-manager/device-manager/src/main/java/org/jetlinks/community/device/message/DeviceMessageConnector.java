package org.jetlinks.community.device.message;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.firmware.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;

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

    public DeviceMessageConnector(EventBus eventBus,
                                  DeviceRegistry registry) {
        this.registry = registry;
        this.eventBus = eventBus;
    }

    public Mono<Void> onMessage(Message message) {
        if (null == message) {
            return Mono.empty();
        }
        return this.getTopic(message)
            .flatMap(topic ->eventBus.publish(topic,message).then())
            .onErrorResume(error -> Mono.fromRunnable(() -> log.error(error.getMessage(), error)))
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
                //获取设备配置是可能存在的性能瓶颈
                .flatMap(operator -> operator.getSelfConfigs(appendConfigHeader))
                .switchIfEmpty(Mono.fromSupplier(() -> Values.of(new HashMap<>())))
                .flatMap(configs -> {
                    configs.getAllValues().forEach(deviceMessage::addHeader);
                    String productId = deviceMessage.getHeader("productId").map(String::valueOf).orElse("null");

                    String topic = String.join("",
                        "/device", "/", productId, "/", deviceId, createDeviceMessageTopic(message)
                    );
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

    public static String createDeviceMessageTopic(Message message) {
        if (message instanceof EventMessage) {   //事件
            EventMessage event = ((EventMessage) message);
            return "/message/event/".concat(event.getEvent());
        } else if (message instanceof ReportPropertyMessage) {   //上报属性
            return "/message/property/report";
        } else if (message instanceof DeviceOnlineMessage) {   //设备上线
            return "/online";
        } else if (message instanceof DeviceOfflineMessage) {   //设备离线
            return "/offline";
        } else if (message instanceof ChildDeviceMessage) { //子设备消息
            Message msg = ((ChildDeviceMessage) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                return "/message/children/".concat(((DeviceMessage) msg).getDeviceId()).concat(createDeviceMessageTopic(msg));
            }
            return "/message/children/".concat(createDeviceMessageTopic(message));
        } else if (message instanceof ChildDeviceMessageReply) { //子设备消息
            Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                return "/message/children/reply/".concat(((DeviceMessage) msg).getDeviceId()).concat(createDeviceMessageTopic(msg));
            }
            return "/message/children/reply/".concat(createDeviceMessageTopic(message));
        } else if (message instanceof ReadPropertyMessage) { //读取属性
            return "/message/send/property/read";
        } else if (message instanceof WritePropertyMessage) { //修改属性
            return "/message/send/property/write";
        } else if (message instanceof FunctionInvokeMessage) { //调用功能
            return "/message/send/function";
        } else if (message instanceof ReadPropertyMessageReply) { //读取属性回复
            return "/message/property/read/reply";
        } else if (message instanceof WritePropertyMessageReply) { //修改属性回复
            return "/message/property/write/reply";
        } else if (message instanceof FunctionInvokeMessageReply) { //调用功能回复
            return "/message/function/reply";
        } else if (message instanceof DeviceRegisterMessage) { //注册
            return "/register";
        } else if (message instanceof DeviceUnRegisterMessage) { //注销
            return "/unregister";
        } else if (message instanceof RequestFirmwareMessage) { //拉取固件请求 since 1.3
            return "/firmware/pull";
        } else if (message instanceof RequestFirmwareMessageReply) { //拉取固件响应 since 1.3
            return "/firmware/pull/reply";
        } else if (message instanceof ReportFirmwareMessage) { //上报固件信息 since 1.3
            return "/firmware/report";
        } else if (message instanceof UpgradeFirmwareProgressMessage) { //上报固件更新进度 since 1.3
            return "/firmware/progress";
        } else if (message instanceof UpgradeFirmwareMessage) { //推送固件更新 since 1.3
            return "/firmware/push";
        } else if (message instanceof UpgradeFirmwareMessageReply) { //推送固件更新回复 since 1.3
            return "/firmware/push/reply";
        } else if (message instanceof DirectDeviceMessage) { //透传消息 since 1.4
            return "/message/direct";
        } else {
            return "/message/unknown";
        }
    }
}
