package org.jetlinks.community.network.mqtt.executor;

import io.netty.buffer.ByteBuf;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MqttRuleDataCodec implements RuleDataCodec<MqttMessage> {

    static {

        MqttRuleDataCodec codec = new MqttRuleDataCodec();
//        EncodedMessageCodec.register(DefaultTransport.MQTT, codec);
//        EncodedMessageCodec.register(DefaultTransport.MQTT_TLS, codec);
        RuleDataCodecs.register(MqttMessage.class, codec);

    }

    static void load() {

    }

    @Override
    public Object encode(MqttMessage message, Feature... features) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", message.getTopic());
        payload.put("will", message.isWill());
        payload.put("qos", message.getQosLevel());
        payload.put("dup", message.isDup());
        payload.put("retain", message.isRetain());
        PayloadType payloadType = Feature.find(PayloadType.class, features).orElse(PayloadType.JSON);
        Feature.find(TopicVariables.class, features)
            .map(TopicVariables::getVariables)
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> list.stream()
                .map(str -> TopicUtils.getPathVariables(str, message.getTopic()))
                .reduce((m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                }))
            .ifPresent(vars -> payload.put("vars", vars));

        payload.put("payloadType", payloadType.name());
        payload.put("payload", payloadType.read(message.getPayload()));
        payload.put("clientId", message.getClientId());


        return payload;
    }

    @Override
    public Flux<MqttMessage> decode(RuleData data, Feature... features) {
        if (data.getData() instanceof MqttMessage) {
            return Flux.just(((MqttMessage) data.getData()));
        }
        MqttTopics topics = Feature.find(MqttTopics.class, features).orElse(null);

        return data
            .dataToMap()
            .filter(map -> map.containsKey("payload"))
            .flatMap(map -> {
                if (topics != null && !map.containsKey("topic")) {
                    return Flux.fromIterable(topics.getTopics())
                        .flatMap(topic -> {
                            Map<String, Object> copy = new HashMap<>();
                            copy.put("topic", topic);
                            copy.putAll(map);
                            return Mono.just(copy);
                        })
                        .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("topic not set")));
                }
                return Flux.just(map);
            })
            .map(map -> {
                PayloadType payloadType = Feature.find(PayloadType.class, features)
                    .orElseGet(() -> Optional.ofNullable(map.get("payloadType"))
                        .map(String::valueOf)
                        .map(PayloadType::valueOf)
                        .orElse(PayloadType.JSON));
                Object payload = map.get("payload");

                ByteBuf byteBuf = payloadType.write(payload);

                Integer qos = (Integer) map.get("qos");

                return SimpleMqttMessage
                    .builder()
                    .clientId((String) map.get("clientId"))
                    .topic((String) map.get("topic"))
                    .dup(Boolean.TRUE.equals(map.get("dup")))
                    .will(Boolean.TRUE.equals(map.get("will")))
                    .retain(Boolean.TRUE.equals(map.get("retain")))
                    .qosLevel(qos == null ? 0 : qos)
                    .payloadType(MessagePayloadType.valueOf(payloadType.name()))
                    .payload(byteBuf)
                    .build();
            });
    }
}