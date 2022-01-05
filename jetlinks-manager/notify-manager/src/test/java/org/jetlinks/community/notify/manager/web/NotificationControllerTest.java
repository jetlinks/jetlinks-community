package org.jetlinks.community.notify.manager.web;

import org.jetlinks.community.notify.manager.entity.NotificationEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.service.NotificationService;
import org.jetlinks.community.notify.manager.service.NotifySubscriberService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebFluxTest(NotifierController.class)
class NotificationControllerTest extends TestJetLinksController {
    private static final String ID= "test";
    private static final String BASE_URL = "/notifications";

    @Autowired
    private NotifySubscriberService subscriberService;

    @Autowired
    private NotificationService notificationService;
    @Test
    @Order(0)
    void save(){
        NotifySubscriberEntity entity = new NotifySubscriberEntity();
        entity.setTopicName("topic");
        entity.setTopicProvider("device_alarm");
        entity.setDescription("test");
        entity.setSubscribeName("test");
        entity.setSubscriberType("user");
        entity.setSubscriber("test");
        entity.setState(SubscribeState.enabled);
        entity.setId(ID);
        entity.setTopicConfig(new HashMap<>());
        assertNotNull(subscriberService);
        subscriberService.save(entity).subscribe();
    }

    @Test
    @Order(1)
    void querySubscription() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/subscriptions/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void changeSubscribeState() {
        assertNotNull(client);
        client.put()
            .uri(BASE_URL+"/subscription/"+ID+"/_enabled")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void deleteSubscription() {
        assertNotNull(client);
        client.delete()
            .uri(BASE_URL+"/subscription/"+ID)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void doSubscribe() {
        NotifySubscriberEntity entity = new NotifySubscriberEntity();
        entity.setTopicName("topic");
        entity.setTopicProvider("device_alarm");
        entity.setDescription("test");
        entity.setSubscribeName("test");
        entity.setId(ID);
        entity.setTopicConfig(new HashMap<>());
        assertNotNull(client);
        client.patch()
            .uri(BASE_URL+"/subscribe")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(entity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getProviders() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/providers")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(0)
    void add(){
        NotificationEntity entity = new NotificationEntity();
        entity.setId(ID);
        entity.setSubscribeId("test");
        entity.setSubscriber("test");
        entity.setTopicName("topic");
        entity.setTopicProvider("device_alarm");
        entity.setDataId("test");
        entity.setState(NotificationState.read);
        entity.setDescription("test");
        entity.setMessage("test");
        entity.setNotifyTime(100L);
        entity.setSubscriberType("user");
        assertNotNull(notificationService);
        notificationService.save(entity).subscribe();
    }


    @Test
    @Order(1)
    void queryMyNotifications() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void readNotification() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/"+ID+"/read")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void testReadNotification() {
        List<String> list = new ArrayList<>();
        list.add(ID);
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/_read")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}