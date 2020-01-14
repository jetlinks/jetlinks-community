package org.jetlinks.community.network.manager.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.web.request.MqttMessageRequest;
import org.jetlinks.community.network.manager.web.response.MqttMessageResponse;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@RestController
@RequestMapping("/network/mqtt")
@Resource(id = "network-config", name = "网络组件配置")
@Slf4j
public class MqttController {

    private final NetworkManager networkManager;

    public MqttController(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /**
     * 订阅MQTT客户端消息
     */
    @GetMapping(value = "/client/{id}/_subscribe/{type}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    public Flux<MqttMessageResponse> subscribe(@PathVariable String id, @PathVariable PayloadType type, @RequestParam String topics) {
        return networkManager
            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, id)
            .flatMapMany(client -> client.subscribe(Arrays.asList(topics.split("[\n]"))))
            .map(msg -> MqttMessageResponse.of(msg, type));
    }

    /**
     * 推送消息
     */
    @PostMapping("/client/{id}/_publish/{type}")
    @SaveAction
    public Mono<Void> publish(@PathVariable String id, @PathVariable PayloadType type, @RequestBody MqttMessageRequest mqttMessage) {
        return networkManager
            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, id)
            .filter(MqttClient::isAlive)
            .switchIfEmpty(Mono.error(() -> new BusinessException("MQTT已断开连接")))
            .flatMap(c -> c.publish(MqttMessageRequest.of(mqttMessage, type)));
    }


    @GetMapping(value = "/server/{id}/_subscribe/{type}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MqttController.MqttClientMessage> serverSubscribe(@PathVariable String id, @PathVariable PayloadType type) {
        return Flux.<MqttController.MqttClientMessage>create(sink -> {
            Disposable disposable = networkManager.<MqttServer>getNetwork(DefaultNetworkType.MQTT_SERVER, id)
                .flatMapMany(MqttServer::handleConnection)
                .flatMap(client -> {
                    client.accept();
                    String clientId = client.getClientId();
                    StringBuffer payload = new StringBuffer("已连接");
                    client.getAuth().ifPresent(mqttAuth -> {
                        payload
                            .append("用户名:")
                            .append(mqttAuth.getUsername())
                            .append(" 密码:")
                            .append(mqttAuth.getPassword());
                    });
                    if (sink.isCancelled()) {
                        return Mono.empty();
                    }
                    sink.next(new MqttController.MqttClientMessage(clientId, payload.toString()));
                    client.onClose(connection -> sink.next(new MqttController.MqttClientMessage(connection.getClientId(), "已断开连接")));
                    return client
                        .handleMessage()
                        .map(msg -> new MqttController.MqttClientMessage(clientId, MqttMessageRequest.of(msg.getMessage(), type)));
                }, 10000)
                .onErrorContinue((err, msg) -> {

                })
                .doOnSubscribe(subscription -> {
                    log.info("subscribe mqtt server [{}] message", id);
                })
                .doFinally((s) -> log.info("cancel subscribe mqtt server [{}] message", id))
                .subscriberContext(sink.currentContext())
                .subscribe(sink::next);
            sink.onCancel(disposable)
                .onDispose(disposable);
        })
            .doOnCancel(() -> log.info("cancel subscribe mqtt server [{}] message", id));
    }

    @Getter
    @AllArgsConstructor
    public static class MqttClientMessage {

        private String clientId;

        private Object payload;
    }


}
