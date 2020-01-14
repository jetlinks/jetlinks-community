package org.jetlinks.community.gateway.rule;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.gateway.TopicMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class TopicMessageCodec implements RuleDataCodec<TopicMessage> {

    private static final TopicMessageCodec INSTANCE = new TopicMessageCodec();

    static {
        RuleDataCodecs.register(TopicMessage.class, INSTANCE);
    }

    public static void register() {
    }


    public static TopicMessageCodec getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<String, Object> encode(TopicMessage data, Feature... features) {

        ByteBuf payload = data.getMessage().getPayload();
        PayloadType payloadType = PayloadType.valueOf(data.getMessage().getPayloadType().name());

        Map<String, Object> map = new HashMap<>();
        map.put("topic", data.getTopic());
        map.put("message", payloadType.read(payload));
        return map;
    }

    @Override
    public Flux<TopicMessage> decode(RuleData data, Feature... features) {

        return Mono.fromSupplier(() -> Feature.find(TopicFeature.class, features)
            .map(TopicFeature::getTopics)
            .orElseThrow(() -> new UnsupportedOperationException("topics not found")))
            .flatMapMany(Flux::just)
            .flatMap(topic -> data
                .dataToMap()
                .map(map -> TopicMessage.of(topic, map)));
    }


    public static TopicFeature feature(String... topics) {
        return new TopicFeature(topics);
    }

    @Getter
    @Setter
    public static class TopicFeature implements RuleDataCodec.Feature {

        private String[] topics;

        public TopicFeature(String... topics) {
            this.topics = topics;
        }
    }

}
