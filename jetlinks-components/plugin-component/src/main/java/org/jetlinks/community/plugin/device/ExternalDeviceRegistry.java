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
package org.jetlinks.community.plugin.device;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.*;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ExternalDeviceRegistry implements DeviceRegistry {

    private final String pluginId;

    private final PluginDataIdMapper idMapper;

    private final DeviceRegistry internal;

    @Override
    public Mono<DeviceOperator> getDevice(String deviceId) {
        return idMapper
            .getInternalId(PluginDataIdMapper.TYPE_DEVICE, pluginId, deviceId)
            .flatMap(internal::getDevice)
            .map(opt -> new ExternalDeviceOperator(deviceId, pluginId, idMapper, opt));
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId) {
        return idMapper
            .getInternalId(PluginDataIdMapper.TYPE_PRODUCT, pluginId, productId)
            .flatMap(internal::getProduct)
            .map(opt -> new ExternalDeviceProductOperator(productId, opt));
    }

    @Override
    public Mono<DeviceOperator> register(DeviceInfo deviceInfo) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<DeviceProductOperator> register(ProductInfo productInfo) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<Void> unregisterDevice(String deviceId) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<Void> unregisterProduct(String productId) {
        return Mono.error(new UnsupportedOperationException());
    }
}
