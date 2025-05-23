/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.device.spi;

import lombok.Generated;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.ConfigScope;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.jetlinks.core.metadata.Feature;
import reactor.core.publisher.Flux;

/**
 * 设备配置定义提供者,通常用于第三方平台接入时,告诉系统对应的产品或者设备所需要的配置，如：第三方平台需要的密钥等信息
 * 系统在导入设备或者编辑设备时，会根据配置定义进行不同的操作，如选择前端界面，生成导出模版等
 *
 * @author zhouhao
 * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager
 * @since 1.7.0
 */
public interface DeviceConfigMetadataSupplier {

    /**
     * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager#getDeviceConfigMetadata(String)
     */
    Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId);

    /**
     * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager#getDeviceConfigMetadataByProductId(String)
     */
    Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId);

    /**
     * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager#getProductConfigMetadata(String)
     */
    Flux<ConfigMetadata> getProductConfigMetadata(String productId);

    /**
     * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager#getProductConfigMetadataByAccessId(String, String)
     */
    @Generated
    default Flux<ConfigMetadata> getProductConfigMetadataByAccessId(String productId, String accessId) {
        return Flux.empty();
    }

    /**
     * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager#getMetadataExpandsConfig(String, DeviceMetadataType, String, String, ConfigScope...)
     */
    @Generated
    default Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                          DeviceMetadataType metadataType,
                                                          String metadataId,
                                                          String typeId) {
        return Flux.empty();
    }

    /**
     * @see org.jetlinks.community.device.service.DeviceConfigMetadataManager#getProductFeatures(String)
     */
    @Generated
    default Flux<Feature> getProductFeatures(String productId){
        return Flux.empty();
    }
}
