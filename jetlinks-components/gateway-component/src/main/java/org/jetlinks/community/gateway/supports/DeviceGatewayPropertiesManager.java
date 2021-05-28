package org.jetlinks.community.gateway.supports;

import reactor.core.publisher.Mono;

/**
 * @author Jetlinks
 */
public interface DeviceGatewayPropertiesManager {

    /**
     * 获取网关的属性
     *
     * @param id 网关ID
     * @return 网关属性
     */
    Mono<DeviceGatewayProperties> getProperties(String id);


}
