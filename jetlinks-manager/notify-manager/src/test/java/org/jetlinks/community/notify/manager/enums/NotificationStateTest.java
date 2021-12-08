package org.jetlinks.community.notify.manager.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationStateTest {

    @Test
    void getValue() {
        String value = NotificationState.read.getValue();
        String text = NotificationState.read.getText();
        assertNotNull(value);
        assertNotNull(text);

        assertNotNull(NotifyState.success.getValue());
        assertNotNull(NotifyState.success.getText());

        assertNotNull(SubscribeState.enabled.getValue());
        assertNotNull(SubscribeState.enabled.getText());
    }
}