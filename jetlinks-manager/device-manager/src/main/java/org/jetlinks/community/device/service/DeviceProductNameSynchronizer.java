package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareCreateEvent;
import org.hswebframework.web.crud.events.EntityPrepareSaveEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动同步产品名称到设备表
 *
 * @author zhouhao
 * @since 1.6
 */
@Component
@AllArgsConstructor
public class DeviceProductNameSynchronizer {

    private final LocalDeviceInstanceService instanceService;
    private final LocalDeviceProductService productService;

    //自动更新产品名称
    @EventListener
    public void autoUpdateProductName(EntityModifyEvent<DeviceProductEntity> event) {
        Map<String, DeviceProductEntity> before = event.getBefore()
                                                       .stream()
                                                       .collect(Collectors.toMap(DeviceProductEntity::getId, Function.identity()));

        event.async(
            Flux.fromIterable(event.getAfter())
                .filter(product -> StringUtils.hasText(product.getName())
                            && before.get(product.getId()) != null && (
                            !Objects.equals(before.get(product.getId()).getName(), product.getName())
                                ||
                                !Objects.equals(before.get(product.getId()).getDeviceType(), product.getDeviceType())
                        )
                )
                .flatMap(product -> instanceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getProductName, product.getName())
                    .set(DeviceInstanceEntity::getDeviceType, product.getDeviceType())
                    .where(DeviceInstanceEntity::getProductId, product.getId())
                    .execute())
        );
    }

    //新增设备前填充产品名称和类型等信息
    @EventListener
    public void autoSetProductInfo(EntityPrepareCreateEvent<DeviceInstanceEntity> event) {

        event.async(
            applyProductToDevice(event.getEntity())
        );
    }

    //新增设备前填充产品名称和类型等信息
    @EventListener
    public void autoSetProductInfo(EntityPrepareSaveEvent<DeviceInstanceEntity> event) {

        event.async(
            applyProductToDevice(event.getEntity())
        );
    }

    protected Mono<Void> applyProductToDevice(Collection<DeviceInstanceEntity> devices) {
        Set<String> productId = devices
            .stream()
            .filter(device -> device.getProductName() == null || device.getDeviceType() == null)
            .map(DeviceInstanceEntity::getProductId)
            .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(productId)) {
            return Mono.empty();
        }

        return productService
            .findById(productId)
            .collectMap(DeviceProductEntity::getId, Function.identity())
            .doOnNext(mapping -> {
                for (DeviceInstanceEntity device : devices) {
                    DeviceProductEntity product = mapping.get(device.getProductId());
                    if (null != product) {
                        device.setProductName(product.getName());
                        device.setDeviceType(product.getDeviceType());
                    }
                }
            })
            .then();
    }

    //自动更新产品名称
    @EventListener
    public void autoUpdateProductName(EntitySavedEvent<DeviceProductEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .filter(product -> StringUtils.hasText(product.getName()))
                .flatMap(product -> instanceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getProductName, product.getName())
                    .set(DeviceInstanceEntity::getDeviceType, product.getDeviceType())
                    .where(DeviceInstanceEntity::getProductId, product.getId())
                    .not(DeviceInstanceEntity::getProductName, product.getName())
                    .execute())
        );
    }
}
