package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.ChildDeviceMessage;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    public Mono<Void> doPublish(Mono<DeviceOperator> device, DeviceMessage message) {
        return device
            .zipWhen(DeviceOperator::getProduct)
            .doOnNext(tp2 -> message.addHeader("productId", tp2.getT2().getId()))
            .flatMap(tp2 -> {
                String topic = DeviceMessageConnector.createDeviceMessageTopic(tp2.getT2().getId(),tp2.getT1().getDeviceId(),message);

                Mono<Void> publisher = eventBus.publish(topic, message).then();

                if (message instanceof ChildDeviceMessage) {
                    DeviceMessage msg = (DeviceMessage) ((ChildDeviceMessage) message).getChildDeviceMessage();
                    publisher = publisher.then(doPublish(registry.getDevice(msg.getDeviceId()), msg));
                }
                return publisher;
            });
    }

    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        return doPublish(Mono.just(device), message)
            .thenReturn(message)
            .doOnEach(ReactiveLogger.onComplete(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("发送指令到设备[{}]:{}", message.getDeviceId(), message.toString());
                }
            }));
    }
}
