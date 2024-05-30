package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityPrepareModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareSaveEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.service.term.DeviceInstanceTerm;
import org.jetlinks.community.things.data.ThingsDataWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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

    private final ThingsDataWriter dataWriter;

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
                        // tp3：新的标签
                        Mono.justOrEmpty(device.getDeriveMetadata())
                            // 设备物模型为空，则获取产品物模型
                            .filter(StringUtils::hasText)
                            .map(metadata -> DeviceTagEntity.parseTagKey(device.getDeriveMetadata()))
                            .switchIfEmpty(operator
                                               .getProduct()
                                               .flatMap(DeviceProductOperator::getMetadata)
                                               .map(DeviceTagEntity::parseTagKey))
                    ))
                .flatMapMany(tp3 -> Flux
                    .fromIterable(tp3.getT1())
                    .filter(tag -> !tp3.getT3().contains(tag))
                    .map(tag -> DeviceTagEntity.createTagId(tp3.getT2(), tag))
                ))
            .as(tagRepository::deleteById)
            .then();
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

    @EventListener
    public void handleDeviceTagEvent(EntityCreatedEvent<DeviceTagEntity> event) {
        event.async(updateTag(event.getEntity()));
    }

    @EventListener
    public void handleDeviceTagEvent(EntitySavedEvent<DeviceTagEntity> event) {
        event.async(updateTag(event.getEntity()));
    }

    /**
     * 更新标签消息
     *
     * @param entityList 标签
     * @return Void
     */
    private Mono<Void> updateTag(List<DeviceTagEntity> entityList) {
        return Flux
            .fromIterable(entityList)
            .flatMap(entity -> dataWriter
                .updateTag(DeviceThingType.device.getId(),
                           entity.getDeviceId(),
                           entity.getKey(),
                           System.currentTimeMillis(),
                           entity.getValue()))
            .then();
    }
}
