package org.jetlinks.community.notify;

import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.notify.event.NotifierEvent;
import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

public class NotifierEventDispatcher<T extends Template> extends NotifierProxy<T> {

    private final MessageGateway gateway;

    public NotifierEventDispatcher(MessageGateway gateway, Notifier<T> target) {
        super(target);
        this.gateway = gateway;
    }

    @Override
    protected Mono<Void> onEvent(NotifierEvent event) {
        // /notify/{notifierId}/success

        return gateway
            .publish(String.join("/", "/notify", event.getNotifierId(), event.isSuccess() ? "success" : "error"), event.toSerializable())
            .then();
    }


}
