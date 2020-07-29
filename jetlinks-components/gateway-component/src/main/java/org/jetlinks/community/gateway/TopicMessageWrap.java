package org.jetlinks.community.gateway;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.NativePayload;
import org.jetlinks.core.Payload;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.message.codec.EncodedMessage;

import javax.annotation.Nonnull;

public class TopicMessageWrap implements TopicMessage {

    private String topic;

    private EncodedMessage message;

    public static TopicMessageWrap wrap(TopicPayload topicPayload) {
        Payload payload = topicPayload.getPayload();
       TopicMessageWrap wrap = new TopicMessageWrap();
        wrap.topic = topicPayload.getTopic();
        if (payload instanceof NativePayload) {
            wrap.message = new EncodableMessage() {
                @Override
                public Object getNativePayload() {
                    return ((NativePayload<?>) payload).getNativeObject();
                }

                @Nonnull
                @Override
                public ByteBuf getPayload() {
                    return payload.getBody();
                }
            };
        } else {
            wrap.message = new EncodedMessage() {
                @Nonnull
                @Override
                public ByteBuf getPayload() {
                    return payload.getBody();
                }
            };
        }
        return wrap;
    }

    @Nonnull
    @Override
    public String getTopic() {
        return topic;
    }

    @Nonnull
    @Override
    public EncodedMessage getMessage() {
        return message;
    }
}
