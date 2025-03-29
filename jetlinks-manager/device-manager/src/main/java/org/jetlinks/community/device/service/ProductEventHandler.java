package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.EntityPrepareModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareSaveEvent;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.configuration.DeviceEventProperties;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author wangsheng
 */
@Component
@AllArgsConstructor
public class ProductEventHandler {

    // 禁用
    private final static byte disabled = 0;

    // 启用
    private final static byte enabled = 1;

    private final DeviceEventProperties properties;

    private final LocalDeviceInstanceService deviceService;

    private final DeviceRegistry registry;


    @EventListener
    public void handleUndeploy(EntityPrepareModifyEvent<DeviceProductEntity> event) {
        if (properties.isOfflineWhenProductDisabled()) {
            event.first(
                handleUndeployProduct(event.getAfter())
            );
        }
    }

    @EventListener
    public void handleUndeploy(EntityPrepareSaveEvent<DeviceProductEntity> event) {
        if (properties.isOfflineWhenProductDisabled()) {
            event.first(
                handleUndeployProduct(event.getEntity())
            );
        }
    }

    /**
     * 产品禁用时使其下设备离线
     *
     * @param productList 产品集合
     */
    private Flux<Void> handleUndeployProduct(List<DeviceProductEntity> productList) {
        return Flux
            .fromIterable(productList)
            .flatMap(this::disableDevice);
    }

    private Mono<Void> disableDevice(DeviceProductEntity product) {
        if (product.getState() != null && product.getState() == disabled) {
            return findDeviceIdByProductId(product.getId())
                .flatMap(deviceId -> registry
                    .getDevice(deviceId)
                    .flatMap(operator -> operator
                        .isOnline()
                        .flatMap(isOnline -> {
                            // 设备在线则断开连接
                            if (isOnline) {
                                return operator.disconnect();
                            }
                            return Mono.empty();
                        }))
                )
                .then();
        }
        return Mono.empty();
    }

    private Flux<String> findDeviceIdByProductId(String productId) {
        return deviceService
            .createQuery()
            .where()
            .and(DeviceInstanceEntity::getProductId, productId)
            .fetch()
            .map(DeviceInstanceEntity::getId);
    }
}
