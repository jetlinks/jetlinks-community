package org.jetlinks.community.network.mqtt.client;

import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.community.network.Network;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MqttClient extends Network {

    Flux<MqttMessage> subscribe(List<String> topics);

    Mono<Void> publish(MqttMessage message);

}
