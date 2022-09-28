package org.jetlinks.community.network.tcp.gateway.device;

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
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class TcpServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;


    public TcpServerDeviceGatewayProvider(NetworkManager networkManager,
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
        return "tcp-server-gateway";
    }

    @Override
    public String getName() {
        return "TCP 透传接入";
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    public Transport getTransport() {
        return DefaultTransport.TCP;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<TcpServer>getNetwork(getNetworkType(), properties.getChannelId())
            .map(mqttServer -> {
                String protocol = properties.getProtocol();

                Assert.hasText(protocol, "protocol can not be empty");

                return new TcpServerDeviceGateway(
                    properties.getId(),
                    Mono.defer(() -> protocolSupports.getProtocol(protocol)),
                    registry,
                    messageHandler,
                    sessionManager,
                    mqttServer
                );
            });
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        TcpServerDeviceGateway deviceGateway = ((TcpServerDeviceGateway) gateway);
        //网络组件发生变化
        if (!Objects.equals(deviceGateway.tcpServer.getId(), properties.getChannelId())) {
            return gateway
                .shutdown()
                .then(this.createDeviceGateway(properties))
                .flatMap(newer -> newer.startup().thenReturn(newer));
        }
        //更新协议
        String protocol = properties.getProtocol();
        deviceGateway.protocol = Mono.defer(() -> protocolSupports.getProtocol(protocol));

        return Mono.just(gateway);
    }
}
