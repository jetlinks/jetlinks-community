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
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityPrepareModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareSaveEvent;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.service.term.DeviceInstanceTerm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * 设备标签自动删除.
 *
 * @author zhangji 2023/3/28
 */
@Component
@ConditionalOnProperty(prefix = "jetlinks.tag.auto-delete", name = "enabled", havingValue = "true", matchIfMissing = true)
@AllArgsConstructor
public class DeviceTagHandler {

    private final DeviceRegistry deviceRegistry;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    /**
     * 更新设备物模型时，若删除了标签，自动删除设备标签
     */
    @EventListener
    public void handleDeviceEvent(EntityPrepareSaveEvent<DeviceInstanceEntity> event) {
        event.async(deleteTagByDevice(event.getEntity()));
    }

    /**
     * 更新设备物模型时，若删除了标签，自动删除设备标签
     */
    @EventListener
    public void handleDeviceEvent(EntityPrepareModifyEvent<DeviceInstanceEntity> event) {
        event.async(deleteTagByDevice(event.getAfter()));
    }

    private Mono<Void> deleteTagByDevice(List<DeviceInstanceEntity> entity) {
        return Flux
            .fromIterable(entity)
            .flatMap(device -> deviceRegistry
                .getDevice(device.getId())
                .flatMap(operator -> Mono
                    .zip(
                        // tp1：旧的标签
                        operator.getMetadata()
                                .map(DeviceTagEntity::parseTagKey),
                        // tp2：设备ID
                        Mono.just(operator.getId()),

                        Mono.zip(
                            getDeviceTags(device),
                            getProductTags(operator),
                            (deviceTags, productTags) -> {
                                deviceTags.addAll(productTags);
                                return deviceTags;
                            }
                        )
                    ))
                .flatMapMany(tp3 -> Flux
                    .fromIterable(tp3.getT1())
                    .filter(tag -> !tp3.getT3().contains(tag))
                    .map(tag -> DeviceTagEntity.createTagId(tp3.getT2(), tag))
                ))
            .as(tagRepository::deleteById)
            .then();
    }

    private Mono<Set<String>> getDeviceTags(DeviceInstanceEntity device) {
        return Mono.justOrEmpty(device.getDeriveMetadata())
                   // 设备物模型为空，则获取产品物模型
                   .filter(StringUtils::hasText)
                   .map(metadata -> DeviceTagEntity.parseTagKey(device.getDeriveMetadata()));
    }

    private Mono<Set<String>> getProductTags(DeviceOperator operator){
        return operator
            .getProduct()
            .flatMap(DeviceProductOperator::getMetadata)
            .map(DeviceTagEntity::parseTagKey);
    }

    /**
     * 更新产品物模型时，若删除了标签，自动删除设备标签
     */
    @EventListener
    public void autoUpdateDeviceTag(EntityPrepareSaveEvent<DeviceProductEntity> event) {
        event.async(deleteTagByProduct(event.getEntity()));
    }

    /**
     * 更新产品物模型时，若删除了标签，自动删除设备标签
     */
    @EventListener
    public void autoUpdateDeviceTag(EntityPrepareModifyEvent<DeviceProductEntity> event) {
        event.async(deleteTagByProduct(event.getAfter()));
    }

    private Mono<Void> deleteTagByProduct(List<DeviceProductEntity> entity) {
        return Flux.fromIterable(entity)
                   .flatMap(product -> deviceRegistry
                       .getProduct(product.getId())
                       .flatMap(productOperator -> Mono
                           .zip(
                               // tp1：旧的产品物模型
                               productOperator
                                   .getMetadata()
                                   .map(DeviceTagEntity::parseTagKey),
                               // tp2：新的产品物模型
                               Mono.just(DeviceTagEntity.parseTagKey(product.getMetadata()))
                               , (oldTags, newTags) -> {
                                   oldTags.removeAll(newTags);
                                   return oldTags;
                               }))
                       .filter(list -> !CollectionUtils.isEmpty(list))
                       .flatMap(tag -> tagRepository
                           .createDelete()
                           .in(DeviceTagEntity::getKey, tag)
                           .and(
                               DeviceTagEntity::getDeviceId,
                               DeviceInstanceTerm.termType,
                               Query
                                   .of()
                                   .is(DeviceInstanceEntity::getProductId, product.getId())
                                   .getParam()
                                   .getTerms())
                           .execute()))
                   .then();
    }


}
