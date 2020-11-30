package org.jetlinks.community.network.mqtt.gateway.device;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Objects;

@Component
public class MqttClientDeviceGatewayProvider implements DeviceGatewayProvider {
    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final ProtocolSupports protocolSupports;

    public MqttClientDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler clientMessageHandler,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.clientMessageHandler = clientMessageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "mqtt-client-gateway";
    }

    @Override
    public String getName() {
        return "MQTT Broker接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<MqttClient>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(mqttClient -> {

                String protocol = (String) properties.getConfiguration().get("protocol");
                String topics = (String) properties.getConfiguration().get("topics");
                Objects.requireNonNull(topics, "topics");

                return new MqttClientDeviceGateway(properties.getId(),
                                                   mqttClient,
                                                   registry,
                                                   protocolSupports,
                                                   protocol,
                                                   sessionManager,
                                                   clientMessageHandler,
                                                   Arrays.asList(topics.split("[,;\n]"))
                );

            });
    }
}
