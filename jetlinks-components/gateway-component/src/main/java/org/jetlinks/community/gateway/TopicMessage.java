package org.jetlinks.community.gateway;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBufUtil;
import org.jetlinks.core.message.codec.EncodedMessage;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

public interface TopicMessage {

    /**
     * 主题: 格式为: /group/1/user/1, 支持通配符: **(多层路径),*(单层路径)
     *
     * <pre>
     *     /group/** , /group/下的全部topic.包括子目录
     *     /group/1/* , /group/1/下的topic. 不包括子目录
     * </pre>
     *
     * @return topic
     */
    @Nonnull
    String getTopic();

    /**
     * @return 已编码的消息
     * @see org.jetlinks.core.message.codec.MqttMessage
     */
    @Nonnull
    EncodedMessage getMessage();

    default Object convertMessage() {
        if (getMessage() instanceof EncodableMessage) {
            return ((EncodableMessage) getMessage()).getNativePayload();
        }
        byte[] payload = getMessage().payloadAsBytes();
        //maybe json
        if (/* { }*/(payload[0] == 123 && payload[payload.length - 1] == 125)
            || /* [ ] */(payload[0] == 91 && payload[payload.length - 1] == 93)
        ) {
            return JSON.parseObject(new String(payload));
        }
        if (ByteBufUtil.isText(getMessage().getPayload(), StandardCharsets.UTF_8)) {
            return getMessage().payloadAsString();
        }
        return payload;
    }

    static TopicMessage of(String topic, EncodedMessage message) {
        return new DefaultTopicMessage(topic, message);
    }

    static TopicMessage of(String topic, Object payload) {
        if (payload instanceof EncodedMessage) {
            return of(topic, ((EncodedMessage) payload));
        }
        return of(topic, EncodableMessage.of(payload));
    }
}
