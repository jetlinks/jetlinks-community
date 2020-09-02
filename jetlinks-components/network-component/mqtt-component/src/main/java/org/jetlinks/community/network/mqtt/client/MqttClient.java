package org.jetlinks.community.network.mqtt.client;

import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.community.network.Network;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MqttClient extends Network {

    default Flux<MqttMessage> subscribe(List<String> topics){
        return subscribe(topics,0);
    }

    Flux<MqttMessage> subscribe(List<String> topics,int qos);

    Mono<Void> publish(MqttMessage message);

}
