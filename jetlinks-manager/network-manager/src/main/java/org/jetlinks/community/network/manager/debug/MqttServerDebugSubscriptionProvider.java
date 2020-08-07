package org.jetlinks.community.network.manager.debug;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.web.response.MqttMessageResponse;
import org.jetlinks.community.network.mqtt.server.*;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MqttServerDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public MqttServerDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-server-mqtt-debug";
    }

    @Override
    public String name() {
        return "MQTT服务调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/mqtt/server/*/_subscribe/*"
        };
    }

    @Override
    public Flux<MqttClientMessage> subscribe(SubscribeRequest request) {
        DebugAuthenticationHandler.handle(request);

        Map<String, String> vars = TopicUtils.getPathVariables("/network/mqtt/server/{id}/_subscribe/{type}", request.getTopic());

        String clientId = vars.get("id");
        PayloadType type = PayloadType.valueOf(vars.get("type").toUpperCase());

        return Flux.create(sink ->
            sink.onDispose(networkManager
                .<MqttServer>getNetwork(DefaultNetworkType.MQTT_SERVER, clientId)
                .flatMap(mqtt ->
                    mqtt
                        .handleConnection()
                        .doOnNext(conn -> {
                            sink.next(MqttClientMessage.of(conn.accept()));
                            conn.onClose(disconnect -> sink.next(MqttClientMessage.ofDisconnect(disconnect)));
                        })
                        .flatMap(conn -> Flux.merge(
                            conn.handleSubscribe(true).map(sub -> MqttClientMessage.of(conn, sub)),
                            conn.handleUnSubscribe(true).map(sub -> MqttClientMessage.of(conn, sub)),
                            conn.handleMessage().map(sub -> MqttClientMessage.of(conn, sub, type)))
                        )
                        .doOnNext(sink::next)
                        .then()
                )
                .doOnError(sink::error)
                .doOnSubscribe(sub -> log.debug("start mqtt server[{}] debug", clientId))
                .doOnCancel(() -> log.debug("stop mqtt server[{}] debug", clientId))
                .subscribe()
            ));
    }


    @AllArgsConstructor(staticName = "of")
    @Getter
    @Setter
    public static class MqttClientMessage {
        private String type;

        private String typeText;

        private Object data;

        public static MqttClientMessage of(MqttConnection connection) {
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", connection.getClientId());
            data.put("address", connection.getClientAddress().toString());
            connection.getAuth().ifPresent(auth -> {
                data.put("username", auth.getUsername());
                data.put("password", auth.getPassword());
            });
            return MqttClientMessage.of("connection", "连接", data);
        }

        public static MqttClientMessage ofDisconnect(MqttConnection connection) {
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", connection.getClientId());
            data.put("address", connection.getClientAddress().toString());
            connection.getAuth().ifPresent(auth -> {
                data.put("username", auth.getUsername());
                data.put("password", auth.getPassword());
            });
            return MqttClientMessage.of("disconnection", "断开连接", data);
        }

        public static MqttClientMessage of(MqttConnection connection, MqttSubscription subscription) {
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", connection.getClientId());
            data.put("address", connection.getClientAddress().toString());
            data.put("topics", subscription
                .getMessage()
                .topicSubscriptions()
                .stream()
                .map(subs -> "QoS:" + subs.qualityOfService().value() + " Topic:" + subs.topicName())
            );
            return MqttClientMessage.of("subscription", "订阅", data);
        }

        public static MqttClientMessage of(MqttConnection connection, MqttUnSubscription subscription) {
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", connection.getClientId());
            data.put("address", connection.getClientAddress().toString());
            data.put("topics", subscription
                .getMessage()
                .topics()
            );
            return MqttClientMessage.of("unsubscription", "取消订阅", data);
        }

        public static MqttClientMessage of(MqttConnection connection, MqttPublishing subscription, PayloadType type) {
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", connection.getClientId());
            data.put("address", connection.getClientAddress().toString());
            data.put("message", MqttMessageResponse.of(subscription.getMessage(), type));
            return MqttClientMessage.of("publish", "推送消息", data);
        }

    }
}
