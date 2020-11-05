package org.jetlinks.community.device.service;

import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Flux;

/**
 * 设备配置信息管理
 *
 * @author zhouhao
 * @since 1.6
 */
public interface DeviceConfigMetadataManager {

    /**
     * 根据设备ID获取配置信息
     *
     * @param deviceId 产品ID
     * @return 配置信息
     */
    Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId);

    /**
     * 根据产品ID获取配置信息
     *
     * @param productId 产品ID
     * @return 配置信息
     */
    Flux<ConfigMetadata> getProductConfigMetadata(String productId);


}
