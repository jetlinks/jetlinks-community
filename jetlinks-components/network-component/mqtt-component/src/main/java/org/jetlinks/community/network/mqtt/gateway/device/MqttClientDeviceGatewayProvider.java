package org.jetlinks.community.network.mqtt.gateway.device;

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
import org.jetlinks.community.network.mqtt.client.MqttClient;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {

        return networkManager
            .<MqttClient>getNetwork(getNetworkType(), properties.getChannelId())
            .map(mqttClient -> {
                String protocol = properties.getProtocol();

                return new MqttClientDeviceGateway(properties.getId(),
                                                   mqttClient,
                                                   registry,
                                                   Mono.defer(() -> protocolSupports.getProtocol(protocol)),
                                                   sessionManager,
                                                   clientMessageHandler
                );

            });
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway, DeviceGatewayProperties properties) {
        MqttClientDeviceGateway deviceGateway = ((MqttClientDeviceGateway) gateway);

        String networkId = properties.getChannelId();
        //网络组件发生了变化
        if (!Objects.equals(networkId, deviceGateway.mqttClient.getId())) {
            return gateway
                .shutdown()
                .then(this
                          .createDeviceGateway(properties)
                          .flatMap(gate -> gate.startup().thenReturn(gate)));
        }
        //更新协议包
        deviceGateway.setProtocol(protocolSupports.getProtocol(properties.getProtocol()));
        return deviceGateway
            .reload()
            .thenReturn(deviceGateway);
    }
}
