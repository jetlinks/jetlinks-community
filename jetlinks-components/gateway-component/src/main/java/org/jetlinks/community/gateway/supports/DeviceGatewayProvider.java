package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.NetworkType;
import reactor.core.publisher.Mono;

public interface DeviceGatewayProvider {

    String getId();

    String getName();

    NetworkType getNetworkType();

    Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties);

}
