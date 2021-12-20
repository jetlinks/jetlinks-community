package org.jetlinks.community.notify.manager.subscriber.providers;

import org.jetlinks.community.notify.manager.subscriber.Notify;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.test.web.TestAuthentication;
import org.jetlinks.core.Payload;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceAlarmProviderTest {

    @Test
    void getConfigMetadata() {
        DeviceAlarmProvider provider = new DeviceAlarmProvider(new BrokerEventBus());
        ConfigMetadata configMetadata = provider.getConfigMetadata();
        assertNotNull(configMetadata);
    }

    @Test
    void createSubscriber() {
        EventBus eventBus = Mockito.mock(EventBus.class);
        DeviceAlarmProvider provider = new DeviceAlarmProvider(eventBus);
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class)))
            .thenReturn(Flux.just(TopicPayload.of("topic",Payload.of(
                "{\"alarmId\":\"test\",\"alarmName\":\"test\",\"deviceName\":\"test\"}"
            ))));
        provider.createSubscriber("test",new TestAuthentication("test"),new HashMap<>())
            .map(Subscriber::subscribe)
            .map(Flux::blockFirst)
            .map(Notify::getDataId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

    }


}