package org.jetlinks.community.rule.engine.configuration;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.community.gateway.EncodableMessage;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.rule.engine.api.NativePayload;
import org.jetlinks.rule.engine.api.SubscribePayload;

import javax.annotation.Nonnull;

@Getter
@Setter
public class EventTopicMessage implements TopicMessage, EncodableMessage {
    private String topic;

    private Object nativePayload;

    private SubscribePayload payload;

    public EventTopicMessage(SubscribePayload payload) {
        this.topic = payload.getTopic();
        this.nativePayload = ((NativePayload) payload.getPayload()).getNativeObject();
        this.payload = payload;
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return payload.getBody();
    }

    @Nonnull
    @Override
    public EncodedMessage getMessage() {
        return this;
    }


}