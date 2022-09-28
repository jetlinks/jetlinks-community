package org.jetlinks.community.notify;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.notify.event.NotifierEvent;
import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

public class NotifierEventDispatcher<T extends Template> extends NotifierProxy<T> {

    private final EventBus eventBus;

    public NotifierEventDispatcher(EventBus eventBus, Notifier<T> target) {
        super(target);
        this.eventBus = eventBus;
    }

    @Override
    protected Mono<Void> onEvent(NotifierEvent event) {
        // /notify/{notifierId}/success

        return eventBus
            .publish(String.join("/", "/notify", event.getNotifierId(), event.isSuccess() ? "success" : "error"), event.toSerializable())
            .then();
    }


}
