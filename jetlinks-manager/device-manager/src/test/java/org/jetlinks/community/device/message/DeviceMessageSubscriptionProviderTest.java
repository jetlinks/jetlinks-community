package org.jetlinks.community.device.message;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceMessageSubscriptionProviderTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void id() {
        DeviceMessageSubscriptionProvider provider = new DeviceMessageSubscriptionProvider(new BrokerEventBus());
        String id = provider.id();
        assertNotNull(id);
        assertEquals("device-message-subscriber",id);
    }

    @Test
    void name() {
        DeviceMessageSubscriptionProvider provider = new DeviceMessageSubscriptionProvider(new BrokerEventBus());
        String name = provider.name();
        assertNotNull(name);
        assertEquals("订阅设备消息",name);
    }

    @Test
    void getTopicPattern() {
        DeviceMessageSubscriptionProvider provider = new DeviceMessageSubscriptionProvider(new BrokerEventBus());
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);
        assertEquals("/device/*/*/**",topicPattern[0]);
    }

    @Test
    void subscribe() {
        SubscribeRequest request = new SubscribeRequest();
        request.setTopic("/device/*/*/**");
        request.setId("test");
        request.setAuthentication(new TestAuthentication("test"));
        EventBus eventBus = Mockito.mock(EventBus.class);
        DeviceMessageSubscriptionProvider provider = new DeviceMessageSubscriptionProvider(eventBus);

        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class)))
            .thenReturn(Flux.just(TopicPayload.of("topic", Payload.of("{'s':'s'}"))));
        provider.subscribe(request).subscribe(Message::getRequestId);
    }
}