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
package org.jetlinks.community.device.service.data;

import lombok.AllArgsConstructor;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.Value;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategies;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
@AllArgsConstructor
public class StorageDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {
    private final DeviceRegistry registry;

    private final DeviceDataStorageProperties properties;

    private ConfigMetadata createObjectConf(Locale locale) {
        return new DefaultConfigMetadata(LocaleUtils.resolveMessage("message.device.storage.config-metadata.name", locale, "存储配置"), "")
            .scope(DeviceConfigScope.product)
            .add(StorageConstants.propertyStorageType, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.name", locale, "存储方式"), getSaveEnumType(locale));
    }

    private ConfigMetadata createAnotherConf(Locale locale) {
        return new DefaultConfigMetadata(LocaleUtils.resolveMessage("message.device.storage.config-metadata.name", locale, "存储配置"), "")
            .scope(DeviceConfigScope.product, DeviceConfigScope.device)
            .add(StorageConstants.propertyStorageType, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.name", locale, "存储方式"), new EnumType()
                .addElement(EnumType.Element.of("direct", LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.direct.name", locale, "直接存储"),
                                                LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.direct.desc", locale, "将上报的属性值保存到配置到存储策略中")))
                .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeIgnore, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.ignore.name", locale, "不存储"),
                                                LocaleUtils.resolveMessage("metadata.device.storage.config-metadata.type.ignore.desc", locale, "不存储此属性值"))));
    }

    private ConfigMetadata createDeviceObjectConf(Locale locale) {
        return new DefaultConfigMetadata(LocaleUtils.resolveMessage("message.device.storage.config-metadata.name", locale, "存储配置"), "")
            .scope(DeviceConfigScope.device)
            .add(StorageConstants.propertyStorageType, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.name", locale, "存储方式"), getSaveEnumType(locale));
    }

    private ConfigMetadata createAnotherDeviceObjectConf(Locale locale) {
        return new DefaultConfigMetadata(LocaleUtils.resolveMessage("message.device.storage.config-metadata.name", locale, "存储配置"), "")
            .scope(DeviceConfigScope.device)
            .add(StorageConstants.propertyStorageType, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.name", locale, "存储方式"), new EnumType()
                .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeIgnore, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.ignore.name", locale, "不存储"),
                                                LocaleUtils.resolveMessage("metadata.device.storage.config-metadata.type.ignore.desc", locale, "不存储此属性值")))
                .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeJson, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.json.name", locale, "JSON字符"),
                                                LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.json.desc", locale, "将数据序列化为JSON字符串进行存储"))));
    }

    private EnumType getSaveEnumType(Locale locale) {
        return new EnumType()
            .addElement(EnumType.Element.of("direct", LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.direct.name", locale, "直接存储"),
                                            LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.direct.desc", locale, "将上报的属性值保存到配置到存储策略中")))
            .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeIgnore, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.ignore.name", locale, "不存储"),
                                            LocaleUtils.resolveMessage("metadata.device.storage.config-metadata.type.ignore.desc", locale, "不存储此属性值")))
            .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeJson, LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.json.name", locale, "JSON字符"),
                                            LocaleUtils.resolveMessage("message.device.storage.config-metadata.type.json.desc", locale, "将数据序列化为JSON字符串进行存储")));
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return Flux.empty();
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
        return Flux.empty();
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return Flux.empty();
    }

    @Override
    public Flux<Feature> getProductFeatures(String productId) {
        return registry
            .getProduct(productId)
            .flatMap(prod -> prod.getConfig(DeviceDataService.STORE_POLICY_CONFIG_KEY))
            .defaultIfEmpty(properties.getDefaultPolicy())
            .flatMap(this::getStoragePolicy)
            .flatMapMany(strategy -> strategy
                .opsForSave(ThingsDataRepositoryStrategy.OperationsContext.DEFAULT)
                .getFeatures());
    }

    private Mono<ThingsDataRepositoryStrategy> getStoragePolicy(String policy) {
        return Mono.justOrEmpty(ThingsDataRepositoryStrategies.getStrategy(policy));
    }

    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String typeId) {
        if (metadataType == DeviceMetadataType.property) {
            return registry
                .getProduct(productId)
                .flatMap(prod -> prod
                    .getConfig(StorageConstants.storePolicyConfigKey)
                    .map(Value::asString))
                .defaultIfEmpty(properties.getDefaultPolicy())
                .flatMapMany(policy -> {
                    if ((ObjectType.ID.equals(typeId) || ArrayType.ID.equals(typeId)) && policy.startsWith("default-")) {
                        if ("default-row".equals(policy)) {
                            // ES行式存储时，只能存在一个对象类型的属性。设备仅支持保存为JSON字符
                            return LocaleUtils
                                .currentReactive()
                                .flatMapMany(locale -> Flux
                                    .just(createObjectConf(locale), createAnotherDeviceObjectConf(locale))
                                );
                        }

                        return LocaleUtils
                            .currentReactive()
                            .flatMapMany(locale -> Flux
                                .just(createObjectConf(locale), createDeviceObjectConf(locale))
                            );}
                    // 存储配置为不存储时，不返回存储配置的定义
                    if (!policy.equals("none")) {
                        return LocaleUtils
                            .currentReactive()
                            .flatMapMany(locale -> Flux.just(createAnotherConf(locale)));
                    }
                    return Flux.empty();
                });
        }

        return Flux.empty();

    }
}
