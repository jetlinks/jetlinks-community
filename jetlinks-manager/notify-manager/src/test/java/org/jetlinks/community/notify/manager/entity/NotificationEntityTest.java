package org.jetlinks.community.notify.manager.entity;

import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationEntityTest {

    @Test
    void from() {
        Notification entity = new Notification();
        entity.setSubscribeId("test");
        entity.setSubscriberType("test");
        entity.setSubscriber("test");
        entity.setTopicProvider("test");
        entity.setMessage("test");
        entity.setDataId("test");
        entity.setNotifyTime(100L);
//        entity.setState(NotificationState.read);
//        entity.setDescription("test");
        NotificationEntity notificationEntity = NotificationEntity.from(entity);
        notificationEntity.setState(NotificationState.read);
        notificationEntity.setDescription("test");
        notificationEntity.setTopicName("test");
        assertNotNull(notificationEntity.getDataId());
        assertNotNull(notificationEntity.getSubscribeId());
        assertNotNull(notificationEntity.getSubscriberType());
        assertNotNull(notificationEntity.getSubscriber());
        assertNotNull(notificationEntity.getTopicName());
        assertNotNull(notificationEntity.getTopicProvider());
        assertNotNull(notificationEntity.getMessage());
        assertNotNull(notificationEntity.getDataId());
        assertNotNull(notificationEntity.getNotifyTime());
        assertNotNull(notificationEntity.getState());
        assertNotNull(notificationEntity.getDescription());
    }
}