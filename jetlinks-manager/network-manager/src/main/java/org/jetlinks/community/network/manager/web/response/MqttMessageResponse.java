package org.jetlinks.community.network.manager.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.rule.engine.executor.PayloadType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageResponse {

    private int messageId;

    private Object payload;

    private String topic;

    private int qosLevel;

    private boolean dup;


    public static MqttMessageResponse of(MqttMessage mqttMessage, PayloadType type) {
        return MqttMessageResponse.builder()
                .dup(mqttMessage.isDup())
                .payload(type.read(mqttMessage.getPayload()))
                .messageId(mqttMessage.getMessageId())
                .qosLevel(mqttMessage.getQosLevel())
                .topic(mqttMessage.getTopic())
                .build();
    }
}
