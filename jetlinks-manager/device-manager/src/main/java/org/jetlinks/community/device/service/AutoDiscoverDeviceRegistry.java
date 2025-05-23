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
package org.jetlinks.community.device.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.*;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * 自动发现设备的注册中心,当默认注册中心没有获取到设备信息时，尝试查询数据库来获取设备信息。
 *
 * @author zhouhao
 */
public class AutoDiscoverDeviceRegistry implements DeviceRegistry {

    private final DeviceRegistry parent;

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;

    private final ReactiveRepository<DeviceProductEntity, String> productRepository;

    public AutoDiscoverDeviceRegistry(DeviceRegistry parent,
                                      ReactiveRepository<DeviceInstanceEntity, String> deviceRepository,
                                      ReactiveRepository<DeviceProductEntity, String> productRepository) {
        this.parent = parent;
        this.deviceRepository = deviceRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Mono<DeviceOperator> getDevice(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return Mono.empty();
        }
        return Mono.defer(() -> parent
            .getDevice(deviceId)
            .switchIfEmpty(Mono.defer(() -> deviceRepository
                .findById(deviceId)
                .filter(instance -> instance.getState() != DeviceState.notActive)
                .flatMap(instance -> parent.register(instance.toDeviceInfo())))
            )
        );
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId) {
        if (StringUtils.isEmpty(productId)) {
            return Mono.empty();
        }
        return parent
            .getProduct(productId)
            .switchIfEmpty(Mono.defer(() -> productRepository
                .findById(productId)
                .filter(product -> product.getState() == 1)
                .flatMap(product -> parent.register(product.toProductInfo()))));
    }

    @Override
    public Mono<DeviceOperator> register(DeviceInfo deviceInfo) {
        return parent.register(deviceInfo);
    }

    @Override
    public Mono<DeviceProductOperator> register(ProductInfo productInfo) {
        return parent.register(productInfo);
    }

    @Override
    public Mono<Void> unregisterDevice(String deviceId) {
        return parent.unregisterDevice(deviceId);
    }

    @Override
    public Mono<Void> unregisterProduct(String productId) {
        return parent.unregisterProduct(productId);
    }

    @Override
    public Mono<Void> unregisterProduct(String productId, String version) {
        return parent.unregisterProduct(productId, version);
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId, String version) {
        return parent.getProduct(productId, version);
    }
}
