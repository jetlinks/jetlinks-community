package org.jetlinks.community.network.manager.web.request;

import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.rule.engine.executor.PayloadType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MqttMessageRequest {

    private String topic;

    private int qosLevel;

    private Object data;

    private int messageId;

    private boolean will;

    private boolean dup;

    private boolean retain;

    public static MqttMessage of(MqttMessageRequest request, PayloadType type) {
        SimpleMqttMessage message = FastBeanCopier.copy(request, new SimpleMqttMessage());
        message.setPayload(type.write(request.getData()));
        return message;
    }

    public static MqttMessageRequest of(MqttMessage message, PayloadType type) {
        MqttMessageRequest requestMessage = new MqttMessageRequest();
        requestMessage.setTopic(message.getTopic());
        requestMessage.setQosLevel(message.getQosLevel());
        requestMessage.setData(type.read(message.getPayload()));
        requestMessage.setWill(message.isWill());
        requestMessage.setDup(message.isDup());
        requestMessage.setRetain(message.isRetain());
        requestMessage.setMessageId(message.getMessageId());
        return requestMessage;
    }

}
