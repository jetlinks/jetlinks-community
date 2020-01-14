package org.jetlinks.community.gateway.supports;

import reactor.core.publisher.Mono;

public interface DeviceGatewayPropertiesManager {

    Mono<DeviceGatewayProperties> getProperties(String id);


}
