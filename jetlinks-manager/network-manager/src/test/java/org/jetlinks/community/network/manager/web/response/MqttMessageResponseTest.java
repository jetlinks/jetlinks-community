package org.jetlinks.community.network.manager.web.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttMessageResponseTest {

    @Test
    void of() {
        MqttMessageResponse response = new MqttMessageResponse(1, "test", "test", 1, true);
        int messageId = response.getMessageId();
        assertEquals(1,messageId);

        int qosLevel = response.getQosLevel();
        assertEquals(1,qosLevel);
        String topic = response.getTopic();
        assertNotNull(topic);
        Object payload = response.getPayload();
        assertNotNull(payload);
        boolean dup = response.isDup();
        assertTrue(dup);
        new NetworkTypeInfo("test","test");

    }
}