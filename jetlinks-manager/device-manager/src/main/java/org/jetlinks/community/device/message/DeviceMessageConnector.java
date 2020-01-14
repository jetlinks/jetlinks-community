package org.jetlinks.community.device.message;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.community.gateway.*;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * 将设备消息连接到消息网关
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class DeviceMessageConnector
    implements MessageConnector,
    MessageConnection,
    MessagePublisher {

    private EmitterProcessor<TopicMessage> messageProcessor = EmitterProcessor.create(false);

    private FluxSink<TopicMessage> sink = messageProcessor.sink();

    //将设备注册中心到配置追加到消息header中,下游订阅者可直接使用.
    private String[] appendConfigHeader = {"productId"};

    //设备注册中心
    private final DeviceRegistry registry;

//    private final DeviceGateway gateway;

    public DeviceMessageConnector(DeviceRegistry registry) {
        this.registry = registry;
    }

    @Nonnull
    @Override
    public String getId() {
        return "device-message-connector";
    }

    @Override
    public String getName() {
        return "设备消息连接器";
    }

    @Override
    public String getDescription() {
        return "连接设备上报的消息到消息网关";
    }

    @Override
    public void onDisconnect(Runnable disconnectListener) {

    }

    @Override
    public void disconnect() {
        messageProcessor.onComplete();
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    public Mono<Void> onMessage(Message message) {
        if (null == message) {
            return Mono.empty();
        }
        if (!messageProcessor.hasDownstreams() && !messageProcessor.isCancelled()) {
            return Mono.empty();
        }

        return this.getTopic(message)
            .map(topic -> TopicMessage.of(topic, message))
            .doOnNext(sink::next)
            .then();
    }


    public Mono<String> getTopic(Message message) {
        if (message instanceof DeviceMessage) {
            DeviceMessage deviceMessage = ((DeviceMessage) message);
            String deviceId = deviceMessage.getDeviceId();
            if (deviceId == null) {
                return Mono.empty();
            }
            return registry
                .getDevice(deviceId)
                //获取设备配置是可能存在的性能瓶颈
                .flatMap(operator -> operator.getSelfConfigs(appendConfigHeader))
                .flatMap(configs -> {
                    configs.getAllValues().forEach(deviceMessage::addHeader);
                    String topic;

                    // TODO: 2019/12/28 自定义topic支持?

                    if (message instanceof EventMessage) {   //事件
                        EventMessage event = ((EventMessage) message);
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/event/".concat(event.getEvent());

                    } else if (message instanceof DeviceOnlineMessage) {   //设备上线
                        topic = "/device/" + deviceMessage.getDeviceId() + "/online";
                    } else if (message instanceof DeviceOfflineMessage) {   //设备离线
                        topic = "/device/" + deviceMessage.getDeviceId() + "/offline";
                    } else if (message instanceof ChildDeviceMessageReply) { //子设备消息
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/child/reply";
                        return onMessage(((ChildDeviceMessageReply) message).getChildDeviceMessage())
                            .thenReturn(topic);
                    } else if (message instanceof ReadPropertyMessage) { //读取属性
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/property/read";
                    } else if (message instanceof WritePropertyMessage) { //修改属性
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/property/write";
                    } else if (message instanceof FunctionInvokeMessage) { //调用功能
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/function/reply";
                    } else if (message instanceof ReadPropertyMessageReply) { //读取属性回复
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/property/read/reply";
                    } else if (message instanceof WritePropertyMessageReply) { //修改属性回复
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/property/write/reply";
                    } else if (message instanceof FunctionInvokeMessageReply) { //调用功能回复
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/function/reply";
                    } else {
                        topic = "/device/" + deviceMessage.getDeviceId() + "/message/unknown";
                    }
                    return Mono.just(topic);
                });

        }
        return Mono.just("/device/unknown/message/unknown");
    }

    @Nonnull
    @Override
    public Flux<MessageConnection> onConnection() {
        return Flux.just(this);
    }

    @Nonnull
    @Override
    public Flux<TopicMessage> onMessage() {
        return messageProcessor.map(Function.identity());
    }
}
