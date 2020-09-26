package org.jetlinks.community.network.manager.debug;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.web.request.MqttMessageRequest;
import org.jetlinks.community.network.manager.web.response.MqttMessageResponse;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Map;

@Component
public class MqttClientDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public MqttClientDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-client-mqtt-debug";
    }

    @Override
    public String name() {
        return "MQTT客户端调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/mqtt/client/*/_subscribe/*",
            "/network/mqtt/client/*/_publish/*"
        };
    }

    @Override
    public Flux<Object> subscribe(SubscribeRequest request) {
        DebugAuthenticationHandler.handle(request);
        Map<String, String> vars = TopicUtils.getPathVariables("/network/mqtt/client/{id}/{pubsub}/{type}", request.getTopic());

        String clientId = vars.get("id");
        String pubsub = vars.get("pubsub");
        PayloadType type = PayloadType.valueOf(vars.get("type").toUpperCase());

        return networkManager
            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, clientId)
            .flatMapMany(mqtt ->
                "_subscribe".equals(pubsub)
                    ? mqttClientSubscribe(mqtt, type, request)
                    : mqttClientPublish(mqtt, type, request))
            ;
    }

    public Flux<Object> mqttClientSubscribe(MqttClient client,
                                            PayloadType type,
                                            SubscribeRequest request) {
        String topics = request.getString("topics", "/#");

        return client
            .subscribe(Arrays.asList(topics.split("[\n]")))
            .map(mqttMessage -> Message.success(request.getId(), request.getTopic(), MqttMessageResponse.of(mqttMessage, type)));

    }

    public Flux<String> mqttClientPublish(MqttClient client,
                                          PayloadType type,
                                          SubscribeRequest request) {
        MqttMessageRequest messageRequest = FastBeanCopier.copy(request.values(), new MqttMessageRequest());

        return client
            .publish(MqttMessageRequest.of(messageRequest, type))
            .thenReturn("推送成功")
            .flux();

    }
}
