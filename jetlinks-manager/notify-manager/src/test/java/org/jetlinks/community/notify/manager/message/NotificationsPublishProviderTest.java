package org.jetlinks.community.notify.manager.message;

import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.test.web.TestAuthentication;
import org.jetlinks.core.Payload;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationsPublishProviderTest {


    @Test
    void id() {
        NotificationsPublishProvider provider = new NotificationsPublishProvider(new BrokerEventBus());
        String id = provider.id();
        assertNotNull(id);
    }

    @Test
    void name() {
        NotificationsPublishProvider provider = new NotificationsPublishProvider(new BrokerEventBus());
        String name = provider.name();
        assertNotNull(name);
    }

    @Test
    void getTopicPattern() {
        NotificationsPublishProvider provider = new NotificationsPublishProvider(new BrokerEventBus());
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);
    }

    @Test
    void subscribe() {
        EventBus eventBus = Mockito.mock(EventBus.class);
        NotificationsPublishProvider provider = new NotificationsPublishProvider(eventBus);
        TopicPayload payload = TopicPayload.of("topic", Payload.of("{'s':'s'}"));
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class)))
            .thenReturn(Flux.just(payload));
        SubscribeRequest request = new SubscribeRequest();
        request.setId("test");
        request.setTopic("topic");
        request.setAuthentication(new TestAuthentication("test"));
        request.setParameter(new HashMap<>());
        provider.subscribe(request)
            .map(Message::getRequestId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }
}