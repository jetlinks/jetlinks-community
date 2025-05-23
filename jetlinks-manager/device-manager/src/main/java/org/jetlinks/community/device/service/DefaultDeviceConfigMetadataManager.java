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

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class DefaultDeviceConfigMetadataManager implements DeviceConfigMetadataManager, BeanPostProcessor {

    private final List<DeviceConfigMetadataSupplier> suppliers = new CopyOnWriteArrayList<>();

    protected void register(DeviceConfigMetadataSupplier supplier) {
        suppliers.add(supplier);
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getDeviceConfigMetadataByProductId(productId))
            .map(config -> config.copy(DeviceConfigScope.device))
            .filter(config -> !CollectionUtils.isEmpty(config.getProperties()))
            .sort(Comparator.comparing(ConfigMetadata::getName))
            .as(FluxTracer.create("/DefaultDeviceConfigMetadataManager/getDeviceConfigMetadataByProductId/" + productId));
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getDeviceConfigMetadata(deviceId))
            .map(config -> config.copy(DeviceConfigScope.device))
            .filter(config -> !CollectionUtils.isEmpty(config.getProperties()))
            .sort(Comparator.comparing(ConfigMetadata::getName))
            .as(FluxTracer.create("/DefaultDeviceConfigMetadataManager/getDeviceConfigMetadata/" + deviceId));
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getProductConfigMetadata(productId))
            .map(config -> config.copy(DeviceConfigScope.product))
            .filter(config -> !CollectionUtils.isEmpty(config.getProperties()))
            .sort(Comparator.comparing(ConfigMetadata::getName))
            .as(FluxTracer.create("/DefaultDeviceConfigMetadataManager/getProductConfigMetadata/" + productId));
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadataByAccessId(String productId,
                                                                   String accessId) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getProductConfigMetadataByAccessId(productId, accessId))
            .map(config -> config.copy(DeviceConfigScope.product))
            .filter(config -> !CollectionUtils.isEmpty(config.getProperties()))
            .sort(Comparator.comparing(ConfigMetadata::getName))
            .as(FluxTracer.create("/DefaultDeviceConfigMetadataManager/getProductConfigMetadataByAccessId/" + productId));
    }

    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String typeId,
                                                         ConfigScope... scopes) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getMetadataExpandsConfig(productId, metadataType, metadataId, typeId))
            .sort(Comparator.comparing(ConfigMetadata::getName))
            .filter(metadata -> metadata.hasAnyScope(scopes))
            .map(metadata -> metadata.copy(scopes))
            .filter(meta -> org.apache.commons.collections4.CollectionUtils.isNotEmpty(meta.getProperties()))
            .as(FluxTracer.create("/DefaultDeviceConfigMetadataManager/getMetadataExpandsConfig/" + productId + "/" + metadataType + ":" + metadataId));
    }

    @Override
    public Flux<Feature> getProductFeatures(String productId) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getProductFeatures(productId))
            .distinct(Feature::getId)
            .as(FluxTracer.create("/DefaultDeviceConfigMetadataManager/getProductFeatures/" + productId));
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
        if (bean instanceof DeviceConfigMetadataSupplier) {
            register(((DeviceConfigMetadataSupplier) bean));
        }
        return bean;
    }
}
