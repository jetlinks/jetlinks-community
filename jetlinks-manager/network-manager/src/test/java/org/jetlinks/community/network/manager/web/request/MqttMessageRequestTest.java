package org.jetlinks.community.network.manager.web.request;

import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MqttMessageRequestTest {

    @Test
    void of() {
        MqttMessageRequest mqttMessageRequest =
            new MqttMessageRequest("test",1,"test",111,true,true,true);
        assertNotNull(mqttMessageRequest);
        SimpleMqttMessage simpleMqttMessage = new SimpleMqttMessage();
        simpleMqttMessage.setTopic("test");
        simpleMqttMessage.setQosLevel(1);
        simpleMqttMessage.setPayload(new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT));
        simpleMqttMessage.setWill(true);
        simpleMqttMessage.setDup(true);
        simpleMqttMessage.setRetain(true);
        simpleMqttMessage.setMessageId(11);
        MqttMessageRequest request = MqttMessageRequest.of(simpleMqttMessage, PayloadType.STRING);
        assertNotNull(request);

    }
}