package org.jetlinks.community.notify.manager.subscriber;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotifyTest {

    @Test
    void getMessage() {
        Notify notify = new Notify();
        notify.setDataId("test");
        notify.setMessage("test");
        notify.setNotifyTime(100L);
        assertNotNull(notify.getMessage());
        assertNotNull(notify.getDataId());
        assertEquals(100L,notify.getNotifyTime());
        Notify.of("test", "test", 100L);
    }
}