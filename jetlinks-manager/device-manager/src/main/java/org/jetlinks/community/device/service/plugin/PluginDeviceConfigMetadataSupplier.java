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
package org.jetlinks.community.device.service.plugin;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Configurable;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.community.device.utils.DeviceCacheUtils;
import org.jetlinks.community.gateway.supports.DeviceGatewayPropertiesManager;
import org.jetlinks.community.gateway.supports.DeviceGatewayProviders;
import org.jetlinks.community.plugin.device.PluginDeviceGatewayProvider;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;

@Component
@AllArgsConstructor
public class PluginDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {
    private final DeviceRegistry registry;
    private final PluginDataIdMapper idMapper;
    private final LocalDeviceInstanceService deviceInstanceService;
    private final LocalDeviceProductService productService;
    private final DeviceGatewayPropertiesManager gatewayPropertiesManager;

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getProduct)
            //注册中心没有则查询数据库
            .switchIfEmpty(
                DeviceCacheUtils
                    .getDeviceOrLoad(deviceId, deviceInstanceService::findById)
                    .map(DeviceInstanceEntity::getProductId)
                    .flatMap(registry::getProduct))
            .flatMapMany(prod -> getByConfigurable(prod, prod.getId(), DeviceGatewayPlugin::getDeviceConfigMetadata));
    }

    protected Flux<ConfigMetadata> getByConfigurable(Configurable configurable,
                                                     String productId,
                                                     BiFunction<DeviceGatewayPlugin, String, Publisher<ConfigMetadata>> mapper) {
        return configurable
            .getConfigs(PropertyConstants.accessProvider, PropertyConstants.accessId)
            .flatMapMany(values -> getByAccessProvider(
                values.getString(PropertyConstants.accessProvider.getKey(), ""),
                values.getString(PropertyConstants.accessId.getKey(), ""),
                productId,
                mapper
            ));
    }

    protected Flux<ConfigMetadata> getByAccessProvider(String accessProvider,
                                                       String accessId,
                                                       String productId,
                                                       BiFunction<DeviceGatewayPlugin, String, Publisher<ConfigMetadata>> mapper) {
        if (!StringUtils.hasText(accessProvider)) {
            return Flux.empty();
            //不应该在这里校验
//            throw new BusinessException("error.product_device_gateway_must_not_be_null", 400);
        }
        return Mono
            .justOrEmpty(
                DeviceGatewayProviders
                    .getProvider(accessProvider)
                    .filter(provider -> provider.isWrapperFor(PluginDeviceGatewayProvider.class))
                    .map(provider -> provider
                        .unwrap(PluginDeviceGatewayProvider.class)
                        .getPlugin(accessId))
            )
            .flatMapMany(plugin -> idMapper
                .getExternalId(PluginDataIdMapper.TYPE_PRODUCT, plugin.getId(), productId)
                .flatMapMany(id -> mapper.apply(plugin, id)));
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
        return registry
            .getProduct(productId)
            .map(prod -> getByConfigurable(prod, prod.getId(), DeviceGatewayPlugin::getDeviceConfigMetadata))
            //注册中心没有则查询数据库
            .defaultIfEmpty(
                DeviceCacheUtils
                    .getProductOrLoad(productId, productService::findById)
                    .filter(product -> StringUtils.hasText(product.getAccessProvider()))
                    .flatMapMany(product -> getByAccessProvider(
                        product.getAccessProvider(),
                        product.getAccessId(),
                        product.getId(),
                        DeviceGatewayPlugin::getDeviceConfigMetadata
                    ))
            )
            .flatMapMany(Function.identity());
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return registry
            .getProduct(productId)
            .map(prod -> getByConfigurable(prod, prod.getId(), DeviceGatewayPlugin::getProductConfigMetadata))
            //注册中心没有则查询数据库
            .defaultIfEmpty(
                DeviceCacheUtils
                    .getProductOrLoad(productId, productService::findById)
                    .filter(product -> StringUtils.hasText(product.getAccessProvider()))
                    .flatMapMany(product -> getByAccessProvider(
                        product.getAccessProvider(),
                        product.getAccessId(),
                        product.getId(),
                        DeviceGatewayPlugin::getProductConfigMetadata
                    ))
            )
            .flatMapMany(Function.identity());
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadataByAccessId(String productId,
                                                                   String accessId) {
        return gatewayPropertiesManager
            .getProperties(accessId)
            .flatMapMany(properties -> getByAccessProvider(
                properties.getProvider(),
                accessId,
                productId,
                DeviceGatewayPlugin::getProductConfigMetadata));
    }
}
