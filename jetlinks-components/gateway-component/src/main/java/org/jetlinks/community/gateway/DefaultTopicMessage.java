package org.jetlinks.community.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;

@Getter
@Setter
@AllArgsConstructor
class DefaultTopicMessage implements TopicMessage {
    private String topic;

    private EncodedMessage message;

    public DefaultTopicMessage(String topic,Object message){
        this.topic=topic;

    }
}
