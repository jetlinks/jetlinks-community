package org.jetlinks.community.notify.manager.message;

import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.notify.manager.test.web.TestAuthentication;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

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
        NotificationsPublishProvider provider = new NotificationsPublishProvider(new BrokerEventBus());
        SubscribeRequest request = new SubscribeRequest();
        request.setId("test");
        request.setTopic("topic");
        request.setAuthentication(new TestAuthentication("test"));
        request.setParameter(new HashMap<>());
        provider.subscribe(request).subscribe();
//            .map(Message::getRequestId)
//            .as(StepVerifier::create)
//            .expectNext("test")
//            .verifyComplete();
    }
}