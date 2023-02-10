package org.jetlinks.community.network.http.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.http.server.HttpServer;
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

/**
 * HTTP 服务设备网关提供商
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
public class HttpServerDeviceGatewayProvider implements DeviceGatewayProvider {
    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final ProtocolSupports protocolSupports;

    public HttpServerDeviceGatewayProvider(NetworkManager networkManager,
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
        return "http-server-gateway";
    }

    @Override
    public String getName() {
        return "HTTP 推送接入";
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<HttpServer>getNetwork(DefaultNetworkType.HTTP_SERVER, properties.getChannelId())
            .map(server -> {
                String protocol = properties.getProtocol();
                return new HttpServerDeviceGateway(properties.getId(),
                                                   server,
                                                   Mono.defer(()->protocolSupports.getProtocol(protocol)),
                                                   sessionManager,
                                                   registry,
                                                   clientMessageHandler);
            });
    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        HttpServerDeviceGateway deviceGateway = ((HttpServerDeviceGateway) gateway);

        String networkId = properties.getChannelId();
        //网络组件发生了变化
        if(!Objects.equals(networkId, deviceGateway.httpServer.getId())){
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

    @Getter
    @Setter
    public static class RouteConfig {
        private String url;

        private String protocol;
    }
}
