package org.jetlinks.community.gateway;

import org.jetlinks.core.message.codec.EncodedMessage;

import javax.annotation.Nonnull;

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
