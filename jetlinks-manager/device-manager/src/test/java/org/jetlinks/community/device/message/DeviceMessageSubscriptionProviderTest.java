package org.jetlinks.community.device.message;

import org.jetlinks.community.device.test.web.TestAuthentication;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

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
        DeviceMessageSubscriptionProvider provider = new DeviceMessageSubscriptionProvider(new BrokerEventBus());

        provider.subscribe(request).subscribe(Message::getRequestId);
    }
}