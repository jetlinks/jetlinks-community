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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.entity.DeviceCategoryEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceFeature;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.community.device.enums.ValidateDataType;
import org.jetlinks.community.gateway.supports.DeviceGatewayProviders;
import org.jetlinks.community.things.ThingsDataRepository;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.jetlinks.supports.utils.DeviceMetadataUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceEntityEventHandler {

    private final LocalDeviceProductService productService;

    private final DeviceRegistry registry;

    private final ProtocolSupports supports;

    private final LocalDeviceInstanceService deviceService;

    private final ThingsDataRepository repository;

    static final String thingType = DeviceThingType.device.getId();

    @EventListener
    public void handleDeviceEvent(EntityPrepareCreateEvent<DeviceInstanceEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::handleMetadata)
                .flatMap(this::checkParentId)
                //新建设备时，若设备没有配置特性，则自动添加协议包中的配置
                .flatMap(this::addDeviceFeature)
        );
    }

    @EventListener
    public void handleDeviceEvent(EntityPrepareSaveEvent<DeviceInstanceEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::handleMetadata)
        );
    }


    @EventListener
    public void handleDeviceEvent(EntityPrepareModifyEvent<DeviceInstanceEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(this::handleMetadata)
        );
    }

    @EventListener
    public void handleDeviceEvent(EntitySavedEvent<DeviceInstanceEntity> event) {
        //保存设备时,自动更新注册中心里的名称和配置信息
        event.first(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::checkParentId, 16)
                .flatMap(device -> registry
                    .getDevice(device.getId())
                    .flatMap(deviceOperator -> {
                        Map<String, Object> configuration =
                            Optional.ofNullable(device.getConfiguration())
                                    .map(HashMap::new)
                                    .orElseGet(HashMap::new);

                        if (StringUtils.hasText(device.getName())) {
                            configuration.put(PropertyConstants.deviceName.getKey(), device.getName());
                        }
                        configuration.put(DeviceConfigKey.parentGatewayId.getKey(), device.getParentId());
                        return deviceOperator.setConfigs(configuration);
                    }))
        );
    }


    @EventListener
    public void handleDeviceEvent(EntityModifyEvent<DeviceInstanceEntity> event) {
        Map<String, DeviceInstanceEntity> olds = event
            .getBefore()
            .stream()
            .filter(device -> StringUtils.hasText(device.getId()))
            .collect(Collectors.toMap(DeviceInstanceEntity::getId, Function.identity()));

        //更新设备时,自动更新注册中心里的名称和配置信息
        event.first(
            Flux.fromIterable(event.getAfter())
                .flatMap(this::checkParentId, 16)
                .flatMap(device -> registry
                    .getDevice(device.getId())
                    .flatMap(deviceOperator -> {
                        Map<String, Object> configuration =
                            Optional.ofNullable(device.getConfiguration())
                                    .map(HashMap::new)
                                    .orElseGet(HashMap::new);

                        DeviceInstanceEntity old = olds.get(device.getId());
                        if (old != null && !Objects.equals(device.getName(), old.getName())) {
                            configuration.put(PropertyConstants.deviceName.getKey(), device.getName());
                        }
                        configuration.put(DeviceConfigKey.parentGatewayId.getKey(), device.getParentId());

                        return deviceOperator.setConfigs(configuration);
                    }))
        );

    }


    //处理派生物模型,移除掉和产品重复的物模型信息.
    private Mono<DeviceInstanceEntity> handleMetadata(DeviceInstanceEntity device) {
        if (StringUtils.hasText(device.getDeriveMetadata())) {
            return validateDeriveMetadata(JetLinksDeviceMetadataCodec
                                              .getInstance()
                                              .doDecode(device.getDeriveMetadata()))
                .flatMap(deriveMetadata -> registry
                    .getProduct(device.getProductId())
                    .flatMap(DeviceProductOperator::getMetadata)
                    .doOnNext(productMetadata -> device
                        .setDeriveMetadata(
                            JetLinksDeviceMetadataCodec
                                .getInstance()
                                .doEncode(
                                    DeviceMetadataUtils.difference(
                                        productMetadata,
                                        deriveMetadata
                                    )
                                )
                        ))
                    .flatMap(productMetadata -> {
                        DeviceMetadata metadata = productMetadata.merge(deriveMetadata);
                        return repository
                            .opsForThing(thingType, device.getId())
                            .flatMap(opt -> opt.forDDL().validateMetadata(metadata));
                    })
                )
                .thenReturn(device);
        }
        return Mono.just(device);
    }


    public Mono<DeviceMetadata> validateDeriveMetadata(DeviceMetadata deriveMetadata) {
        return this
            .validateEvents(deriveMetadata)
            .then(validateFunction(deriveMetadata))
            .then(validateProperties(deriveMetadata))
            .then(validateTags(deriveMetadata))
            .thenReturn(deriveMetadata);
    }

    private Mono<Void> validateEvents(DeviceMetadata deviceMetadata) {
        return Flux
            .fromIterable(deviceMetadata.getEvents())
            .flatMap(ValidateDataType::validateIdAndName)
            .then();
    }

    private Mono<Void> validateFunction(DeviceMetadata deviceMetadata) {
        return Flux
            .fromIterable(deviceMetadata.getFunctions())
            .flatMap(ValidateDataType::validateIdAndName)
            .then();
    }

    private Mono<Void> validateProperties(DeviceMetadata deviceMetadata) {
        return Flux
            .fromIterable(deviceMetadata.getProperties())
            .flatMap(property -> ValidateDataType.validateIdAndName(property)
                                                 .then(ValidateDataType.handleValidateDataType(property.getValueType(), property.getId()))
            )
            .then();
    }

    private Mono<Void> validateTags(DeviceMetadata deviceMetadata) {
        return Flux
            .fromIterable(deviceMetadata.getTags())
            .flatMap(ValidateDataType::validateIdAndName)
            .then();
    }


    @EventListener
    public void handleDeviceDeleteEvent(EntityDeletedEvent<DeviceInstanceEntity> event) {
        event.async(
            // 删除设备后，解绑子设备
            Flux
                .fromIterable(event.getEntity())
                // 只处理网关设备
                .filter(entity -> entity.getDeviceType() == DeviceType.gateway)
                .map(DeviceInstanceEntity::getId)
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(deviceIdList -> deviceService
                    .createUpdate()
                    .setNull(DeviceInstanceEntity::getParentId)
                    .in(DeviceInstanceEntity::getParentId, deviceIdList)
                    .execute())
                .then()
        );
    }


    private Mono<DeviceInstanceEntity> checkParentId(DeviceInstanceEntity device) {
        if (StringUtils.hasText(device.getId()) && Objects.equals(device.getId(), device.getParentId())) {
            return Mono.error(new BusinessException("error.device_id_and_parent_id_can_not_be_the_same", 500, device.getId()));
        }
        return deviceService
            .checkCyclicDependency(device)
            .thenReturn(device);
    }

    private Mono<Void> addDeviceFeature(DeviceInstanceEntity device) {
        // 已配置feature，则不覆盖
        if (StringUtils.hasText(device.getProductId()) &&
            (device.getFeatures() == null || device.getFeatures().length == 0)) {
            return registry
                .getProduct(device.getProductId())
                .flatMap(product -> Mono
                    .zip(
                        // 协议
                        product.getProtocol(),
                        // 设备接入方式
                        product
                            .getConfig(PropertyConstants.accessProvider)
                            .flatMap(provider -> Mono
                                .justOrEmpty(DeviceGatewayProviders.getProvider(provider)))
                    )
                    .flatMapMany(tp2 -> tp2
                        .getT1()
                        .getFeatures(tp2.getT2().getTransport())
                        .doOnNext(feature -> {
                            DeviceFeature deviceFeature = DeviceFeature.get(feature.getId());
                            if (deviceFeature != null) {
                                device.addFeature(deviceFeature);
                            }
                        }))
                    .then()
                    .onErrorResume(err -> {
                        log.warn("auto set device[{}] default feature error", device.getName(), err);
                        return Mono.empty();
                    }));
        }

        return Mono.empty();
    }

    @EventListener
    public void handleProductDefaultMetadata(EntityPrepareCreateEvent<DeviceProductEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(product -> {
                    //新建产品时自动填充默认物模型
                    if (product.getMetadata() == null &&
                        StringUtils.hasText(product.getMessageProtocol()) &&
                        StringUtils.hasText(product.getTransportProtocol())) {
                        return supports
                            .getProtocol(product.getMessageProtocol())
                            .flatMapMany(support -> support
                                .getDefaultMetadata(Transport.of(product.getTransportProtocol()))
                                .flatMap(JetLinksDeviceMetadataCodec.getInstance()::encode)
                                .doOnNext(product::setMetadata))
                            .onErrorResume(err -> {
                                log.warn("auto set product[{}] default metadata error", product.getName(), err);
                                return Mono.empty();
                            });
                    }
                    return Mono.empty();
                })
        );
    }

    @EventListener
    public void handleCategoryDelete(EntityDeletedEvent<DeviceCategoryEntity> event) {
        //修改分类关联的产品
        event.async(
            productService
                .createUpdate()
                .setNull(DeviceProductEntity::getClassifiedId)
                .in(DeviceProductEntity::getClassifiedId, event
                    .getEntity()
                    .stream()
                    .map(DeviceCategoryEntity::getId)
                    .collect(Collectors.toList()))
                .execute()
                .as(EntityEventHelper::setDoNotFireEvent)
        );

    }

    //修改产品分类时，同步修改产品分类名称
    @EventListener
    public void handleCategorySave(EntitySavedEvent<DeviceCategoryEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .concatMap(category -> productService
                    .createUpdate()
                    .set(DeviceProductEntity::getClassifiedName, category.getName())
                    .where(DeviceProductEntity::getClassifiedId, category.getId())
                    .execute()
                    .then()
                    .as(EntityEventHelper::setDoNotFireEvent))
        );
    }
}
