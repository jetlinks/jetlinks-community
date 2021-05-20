package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.PropertyMetadataConstants;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.ChildDeviceMessage;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * 发送设备指令的时候,将消息推送到网关中.
 *
 * @author zhouhao
 * @since 1.1
 */
@Component
@Slf4j(topic = "system.device.message.sender")
@AllArgsConstructor
public class DeviceMessageSendLogInterceptor implements DeviceMessageSenderInterceptor {

    private final EventBus eventBus;

    private final DeviceRegistry registry;

    public Mono<Void> doPublish(Message message) {
        return DeviceMessageConnector
            .createDeviceMessageTopic(registry, message)
            .flatMap(topic -> {
                Mono<Void> publisher = eventBus.publish(topic, message).then();
                if (message instanceof ChildDeviceMessage) {
                    publisher = publisher.then(doPublish(((ChildDeviceMessage) message).getChildDeviceMessage()));
                }
                return publisher;
            })
            .then();
    }

    @Override
    public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {
        if (message instanceof WritePropertyMessage) {
            Map<String, Object> properties =((WritePropertyMessage) message).getProperties();
            if (properties.size() == 1) {
                String property = properties.keySet().iterator().next();
                Object value = properties.values().iterator().next();
                //手动写值的属性则直接返回
                return device
                    .getMetadata()
                    .flatMap(metadata -> Mono
                        .justOrEmpty(
                            metadata
                                .getProperty(property)
                                .filter(PropertyMetadataConstants.Source::isManual)
                                .map(ignore -> ((WritePropertyMessage) message)
                                    .newReply()
                                    .addHeader("source", PropertyMetadataConstants.Source.manual)
                                    .addProperty(property, value)
                                    .success()
                                )
                        ))
                    .map(replyMsg -> this.doPublish(replyMsg).thenReturn((R) replyMsg).flux())
                    .defaultIfEmpty(reply)
                    .flatMapMany(Function.identity());
            }
        }
        return reply;
    }

    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        return this
            .doPublish(message )
            .thenReturn(message)
            .doOnEach(ReactiveLogger.onComplete(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("发送指令到设备[{}]:{}", message.getDeviceId(), message.toString());
                }
            }));
    }
}
