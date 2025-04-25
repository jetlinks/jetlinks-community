package org.jetlinks.community.event;

import lombok.AllArgsConstructor;
import org.jetlinks.core.event.EventBus;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 推送系统事件到事件总线，topic: /sys-event/{operationType}/{operationId}/{level}
 *
 * @author zhouhao
 * @since 2.0
 */
@AllArgsConstructor
public class SystemEventDispatcher implements SystemEventHandler {

    private final EventBus eventBus;

    private final ApplicationEventPublisher eventPublisher;


    @Override
    public final void handle(SystemEvent event) {

        String topic = SystemEventHandler
            .topic(event.getOperation().getType().getId(),
                   event.getOperation().getSource().getId(),
                   event.getLevel().name());

        eventPublisher.publishEvent(event);

        OperationAssetProvider provider = OperationAssetProviders
            .lookup(event.getOperation())
            .orElse(null);

        //对数据权限控制的支持
        if (provider != null) {
            provider
                .createTopics(event.getOperation(), topic)
                .flatMap(_topic -> eventBus.publish(_topic, event))
                .subscribe();
        } else {
            eventBus.publish(topic, event)
                    .subscribe();
        }


    }
}
