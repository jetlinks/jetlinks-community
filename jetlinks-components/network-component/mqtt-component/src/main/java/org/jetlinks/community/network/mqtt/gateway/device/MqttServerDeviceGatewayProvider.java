package org.jetlinks.community.network.mqtt.gateway.device;

import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class MqttServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    public MqttServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler messageHandler,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "mqtt-server-gateway";
    }

    @Override
    public String getName() {
        return "MQTT直连接入";
    }


    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {

        return networkManager
            .<MqttServer>getNetwork(getNetworkType(), properties.getChannelId())
            .map(mqttServer -> new MqttServerDeviceGateway(
                properties.getId(),
                registry,
                sessionManager,
                mqttServer,
                messageHandler,
                Mono.empty()
            ));
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        MqttServerDeviceGateway deviceGateway = ((MqttServerDeviceGateway) gateway);

        String networkId = properties.getChannelId();
        //网络组件发生了变化
        if (!Objects.equals(networkId, deviceGateway.getMqttServer().getId())) {
            return gateway
                .shutdown()
                .then(this
                    .createDeviceGateway(properties)
                    .flatMap(gate -> gate.startup().thenReturn(gate)));
        }
        return Mono.just(gateway);
    }
}
