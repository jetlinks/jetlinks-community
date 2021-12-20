package org.jetlinks.community.notify.manager.entity;

import org.jetlinks.community.notify.manager.subscriber.Notify;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationTest {

    @Test
    void copyWithMessage() {
        Notification notification = new Notification();
        Notify notify = new Notify();
        notify.setDataId("test");
        notify.setMessage("test");
        notify.setNotifyTime(100L);
        Notification message = notification.copyWithMessage(notify);
        assertNotNull(message.getId());
        assertNotNull(message.getMessage());
        assertEquals(100L,message.getNotifyTime());
        assertNotNull(message.getDataId());
    }

}