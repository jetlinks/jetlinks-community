package org.jetlinks.community.gateway.supports;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 设备网关属性管理器
 *
 * @author zhouhao
 */
public interface DeviceGatewayPropertiesManager {

    /**
     * 获取网关的属性
     *
     * @param id 网关ID
     * @return 网关属性
     */
    Mono<DeviceGatewayProperties> getProperties(String id);

    Flux<DeviceGatewayProperties> getPropertiesByChannel(String channel);

}
