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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareSaveEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * @author bestfeng
 */
@AllArgsConstructor
@Component
public class DeviceProductHandler {

    private final LocalDeviceProductService productService;

    private final LocalDeviceInstanceService instanceService;

    private final DeviceRegistry deviceRegistry;

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleProductSaveEvent(EntitySavedEvent<DeviceProductEntity> event) {
        event.async(
            applyProductConfig(event.getEntity())
        );
    }

    @EventListener
    public void handleProductSaveEvent(EntityModifyEvent<DeviceProductEntity> event) {
        event.async(
            applyProductConfig(event.getBefore())
        );
    }

    //已发布状态的产品配置更新后，重新应用配置
    private Mono<Void> applyProductConfig(List<DeviceProductEntity> entities) {
        return Flux
            .fromIterable(entities)
            .map(DeviceProductEntity::getId)
            .as(productService::findById)
            .filter(product -> product.getState() == 1)
            .flatMap(product -> deviceRegistry
                         .register(product.toProductInfo())
                         .flatMap(i -> FastBeanCopier
                             .copy(product, new DeviceProductDeployEvent())
                             .publish(eventPublisher)),
                     8)
            .then();
    }

    @EventListener
    public void handleProductSaveEvent(EntityPrepareSaveEvent<DeviceProductEntity> event) {
        event.async(
            Flux
                .fromIterable(event.getEntity())
                .mapNotNull(DeviceProductEntity::getId)
                .as(productService::findById)
                .collectList()
                .flatMap(before -> checkAccessIdChange(before, event.getEntity()))
        );
    }

    @EventListener
    public void handleProductSaveEvent(EntityPrepareModifyEvent<DeviceProductEntity> event) {
        event.async(
            checkAccessIdChange(event.getBefore(), event.getAfter())
        );
    }

    /**
     * 检查产品接入方式
     * 当产品下有设备时，禁止修改接入方式
     *
     * @param before 修改前产品
     * @param after  修改后产品
     */
    private Mono<Void> checkAccessIdChange(List<DeviceProductEntity> before, List<DeviceProductEntity> after) {
        return Flux
            .fromIterable(before)
            .collectMap(DeviceProductEntity::getId)
            .filter(MapUtils::isNotEmpty)
            .flatMap(beforeMap -> Flux
                .fromIterable(after)
                // 过滤出修改了接入方式的产品
                .filter(entity -> !Objects.equals(entity.getAccessId(), beforeMap.get(entity.getId()).getAccessId())
                    || !Objects.equals(entity.getAccessProvider(), (beforeMap.get(entity.getId()).getAccessProvider())))
                .map(DeviceProductEntity::getId)
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(productId -> instanceService
                    .createQuery()
                    .in(DeviceInstanceEntity::getProductId, productId)
                    .count()
                    .flatMap(deviceNum -> {
                        // 产品下有设备时，禁止修改接入方式
                        if (deviceNum > 0) {
                            return Mono.error(
                                () -> new BusinessException("error.product_access_id_can_not_change_with_device")
                            );
                        }
                        return Mono.empty();
                    }))
                .then());
    }
}
