package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author bestfeng
 */
@AllArgsConstructor
@Component
public class DeviceProductHandler {

    private final LocalDeviceProductService productService;

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
                    .publish(eventPublisher))
            )
            .then();
    }
}
