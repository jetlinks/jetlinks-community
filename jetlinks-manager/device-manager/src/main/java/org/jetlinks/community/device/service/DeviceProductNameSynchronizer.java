package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.crud.events.*;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.community.PropertyConstants;
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
@Slf4j
@AllArgsConstructor
public class DeviceProductNameSynchronizer {

    private final LocalDeviceInstanceService instanceService;
    private final LocalDeviceProductService productService;

    private final DeviceRegistry registry;

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
                                !Objects.equals(before
                                                    .get(product.getId())
                                                    .getDeviceType(), product.getDeviceType())
                        )
                )
                .flatMap(product -> instanceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getProductName, product.getName())
                    .set(DeviceInstanceEntity::getDeviceType, product.getDeviceType())
                    .where(DeviceInstanceEntity::getProductId, product.getId())
                    .execute()
                    //不触发事件，设备数量较多时，性能较差。
                    .as(EntityEventHelper::setDoNotFireEvent)
                    .then(Objects.equals(before.get(product.getId()).getName(), product.getName())
                              ? Mono.empty()
                              : syncDeviceProductName(product.getId(), product.getName()))
                ));
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

        event.async(applyProductToDevice(event.getEntity()));
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
                             .execute()
                             //不触发事件，设备数量较多时，性能较差。
                             .as(EntityEventHelper::setDoNotFireEvent)
                             .then(syncDeviceProductName(product.getId(), product.getName())
                             )
                    , 8)
        );
    }

    private Mono<Void> syncDeviceProductName(String productId, String name) {
        return Mono.fromRunnable(() -> syncDeviceProductNameAsync(productId, name));
    }

    @SuppressWarnings("all")
    private void syncDeviceProductNameAsync(String productId, String name) {
        registry
            .getProduct(productId)
            .flatMap(product -> product
                .getDevices()
                .flatMap(device -> device.setConfig(PropertyConstants.productName, name))
                .then())
            .as(MonoTracer.create("/product/" + productId + "/sync-device-name"))
            .subscribe(null,
                       err -> log.warn("sync device product [{}] name error", productId, err));
    }
}
