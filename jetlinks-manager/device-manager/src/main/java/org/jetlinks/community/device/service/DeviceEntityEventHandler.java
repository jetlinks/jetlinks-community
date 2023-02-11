package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntityPrepareCreateEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.entity.DeviceCategoryEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceEntityEventHandler {

    private final LocalDeviceProductService productService;

    private final DeviceRegistry registry;

    private final ProtocolSupports supports;

    @EventListener
    public void handleDeviceEvent(EntitySavedEvent<DeviceInstanceEntity> event) {
        //保存设备时,自动更新注册中心里的名称
        event.first(
            Flux.fromIterable(event.getEntity())
                .filter(device -> StringUtils.hasText(device.getName()))
                .flatMap(device -> registry
                    .getDevice(device.getId())
                    .flatMap(deviceOperator -> deviceOperator.setConfig(PropertyConstants.deviceName, device.getName())))
        );
    }

    @EventListener
    public void handleDeviceEvent(EntityModifyEvent<DeviceInstanceEntity> event) {
        Map<String, DeviceInstanceEntity> olds = event
            .getBefore()
            .stream()
            .filter(device -> StringUtils.hasText(device.getId()))
            .collect(Collectors.toMap(DeviceInstanceEntity::getId, Function.identity()));

        //更新设备时,自动更新注册中心里的名称
        event.first(
            Flux.fromIterable(event.getAfter())
                .filter(device -> {
                    DeviceInstanceEntity old = olds.get(device.getId());
                    return old != null && !Objects.equals(device.getName(), old.getName());
                })
                .flatMap(device -> registry
                    .getDevice(device.getId())
                    .flatMap(deviceOperator -> deviceOperator.setConfig(PropertyConstants.deviceName, device.getName())))
        );

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
        //禁止删除有产品使用的分类
        event.async(
            productService
                .createQuery()
                .in(DeviceProductEntity::getClassifiedId, event
                    .getEntity()
                    .stream()
                    .map(DeviceCategoryEntity::getId)
                    .collect(Collectors.toList()))
                .count()
                .doOnNext(i -> {
                    if (i > 0) {
                        throw new BusinessException("error.device_category_has_bean_use_by_product");
                    }
                })
        );

    }

    //修改产品分类时，同步修改产品分类名称
    @EventListener
    public void handleCategorySave(EntitySavedEvent<DeviceCategoryEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(category -> productService
                    .createUpdate()
                    .set(DeviceProductEntity::getClassifiedName, category.getName())
                    .where(DeviceProductEntity::getClassifiedId, category.getId())
                    .execute()
                    .then())
        );
    }
}
