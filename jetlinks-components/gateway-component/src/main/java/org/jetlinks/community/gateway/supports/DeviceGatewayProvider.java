package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.NetworkType;
import reactor.core.publisher.Mono;

/**
 * 设备网关服务提供者
 *
 * @author Jetlinks
 */
public interface DeviceGatewayProvider {

    String getId();

    String getName();

    NetworkType getNetworkType();

    Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties);

}
