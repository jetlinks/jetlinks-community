package org.jetlinks.community.network.mqtt.gateway.device;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttClientSession;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.route.MqttRoute;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.GatewayState;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.community.network.mqtt.gateway.device.session.UnknownDeviceMqttClientSession;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT Client 设备网关，使用网络组件中的MQTT Client来处理设备数据
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class MqttClientDeviceGateway extends AbstractDeviceGateway {

    final MqttClient mqttClient;

    private final DeviceRegistry registry;

    private Mono<ProtocolSupport> protocol;

    private Mono<DeviceMessageCodec> codecMono;

    private final DeviceGatewayHelper helper;

    private final Map<RouteKey, Tuple2<Integer, Disposable>> routes = new ConcurrentHashMap<>();

    public MqttClientDeviceGateway(String id,
                                   MqttClient mqttClient,
                                   DeviceRegistry registry,
                                   Mono<ProtocolSupport> protocol,
                                   DeviceSessionManager sessionManager,
                                   DecodedClientMessageHandler clientMessageHandler) {
        super(id);
        this.mqttClient = Objects.requireNonNull(mqttClient, "mqttClient");
        this.registry = Objects.requireNonNull(registry, "registry");
        setProtocol(protocol);
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    protected Mono<ProtocolSupport> getProtocol() {
        return protocol;
    }

    public void setProtocol(Mono<ProtocolSupport> protocol) {
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.codecMono = protocol.flatMap(p -> p.getMessageCodec(getTransport()));
    }

    protected Mono<Void> reload() {

        return this
            .getProtocol()
            .flatMap(support -> support
                .getRoutes(DefaultTransport.MQTT)
                .filter(MqttRoute.class::isInstance)
                .cast(MqttRoute.class)
                .collectList()
                .doOnEach(ReactiveLogger
                              .onNext(routes -> {
                                  //协议包里没有配置Mqtt Topic信息
                                  if (CollectionUtils.isEmpty(routes)) {
                                      log.warn("The protocol [{}] is not configured with topics information", support.getId());
                                  }
                              }))
                .doOnNext(this::doReloadRoute))
            .then();
    }

    protected void doReloadRoute(List<MqttRoute> routes) {
        Map<RouteKey, Tuple2<Integer, Disposable>> readyToRemove = new HashMap<>(this.routes);

        for (MqttRoute route : routes) {
            //不是上行topic,不订阅
            if (!route.isUpstream()) {
                continue;
            }
            String topic = convertToMqttTopic(route.getTopic());
            RouteKey key = RouteKey.of(topic, route.getQos());
            readyToRemove.remove(key);
            //尝试更新订阅
            this.routes.compute(key, (_key, old) -> {
                if (old != null) {
                    //QoS没变，不用重新订阅
                    if (old.getT1().equals(_key.qos)) {
                        return old;
                    } else {
                        old.getT2().dispose();

                    }
                }
                return Tuples.of(_key.qos, doSubscribe(_key.topic, _key.qos));
            });
        }
        //取消订阅协议包里没有的topic信息
        for (Map.Entry<RouteKey, Tuple2<Integer, Disposable>> value : readyToRemove.entrySet()) {
            this.routes.remove(value.getKey());
            value.getValue().getT2().dispose();
        }
    }

    protected static String convertToMqttTopic(String topic) {
        return TopicUtils.convertToMqttTopic(topic);
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    protected Disposable doSubscribe(String topic, int qos) {
        return mqttClient
            .subscribe(Collections.singletonList(topic), qos)
            .filter(msg -> isStarted())
            .flatMap(mqttMessage -> codecMono
                .flatMapMany(codec -> codec
                    .decode(FromDeviceMessageContext.of(
                        new UnknownDeviceMqttClientSession(getId(), mqttClient, monitor),
                        mqttMessage,
                        registry,
                        msg -> handleMessage(mqttMessage, msg).then())))
                .cast(DeviceMessage.class)
                .concatMap(message -> handleMessage(mqttMessage, message))
                .subscribeOn(Schedulers.parallel())
                .onErrorResume((err) -> {
                    log.error("handle mqtt client message error:{}", mqttMessage, err);
                    return Mono.empty();
                }), Integer.MAX_VALUE)
            .contextWrite(ReactiveLogger.start("gatewayId", getId()))
            .subscribe();
    }

    private Mono<Void> handleMessage(MqttMessage mqttMessage, DeviceMessage message) {
        monitor.receivedMessage();
        return helper
            .handleDeviceMessage(message,
                                 device -> createDeviceSession(device, mqttClient),
                                 ignore -> {
                                 },
                                 () -> log.warn("can not get device info from message:{},{}", mqttMessage.print(), message)
            )
            .then();
    }

    @AllArgsConstructor(staticName = "of")
    @EqualsAndHashCode(exclude = "qos")
    private static class RouteKey {
        private String topic;
        private int qos;
    }

    private MqttClientSession createDeviceSession(DeviceOperator device, MqttClient client) {
        return new MqttClientSession(device.getDeviceId(), device, client, monitor);
    }

    @Override
    protected Mono<Void> doShutdown() {
        for (Tuple2<Integer, Disposable> value : routes.values()) {
            value.getT2().dispose();
        }
        routes.clear();
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doStartup() {
        return reload();
    }
}
