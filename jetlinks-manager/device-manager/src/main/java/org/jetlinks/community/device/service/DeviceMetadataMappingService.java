package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceMetadataMappingDetail;
import org.jetlinks.community.device.entity.DeviceMetadataMappingEntity;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DeviceMetadataMappingService extends GenericReactiveCrudService<DeviceMetadataMappingEntity, String> {

    private final DeviceRegistry registry;

    private final LocalDeviceProductService productService;

    private final LocalDeviceInstanceService deviceService;

    public Flux<DeviceMetadataMappingDetail> getProductMappingDetail(String productId) {
        return this
            .getProductMetadata(productId)
            .flatMapMany(metadata -> this
                .convertDetail(metadata,
                               this
                                   .createQuery()
                                   .where(DeviceMetadataMappingEntity::getProductId, productId)
                                   .isNull(DeviceMetadataMappingEntity::getDeviceId)
                                   .fetch(),
                               () -> DeviceMetadataMappingDetail.ofProduct(productId)
                ));
    }

    public Flux<DeviceMetadataMappingDetail> getDeviceMappingDetail(String deviceId) {
        return deviceService
            .findById(deviceId)
            .flatMapMany(device -> this
                .getDeviceMetadata(device)
                .flatMapMany(metadata -> this
                    .convertDetail(
                        metadata,
                        this
                            .createQuery()
                            //where product_id =? and (device_id is null or device_id = ?)
                            .where(DeviceMetadataMappingEntity::getProductId, device.getProductId())
                            .nest()
                            .isNull(DeviceMetadataMappingEntity::getDeviceId)
                            .or()
                            .is(DeviceMetadataMappingEntity::getDeviceId, deviceId)
                            .end()
                            .fetch(),
                        () -> DeviceMetadataMappingDetail.ofDevice(device.getProductId(), deviceId))
                ));
    }

    public Mono<Void> saveDeviceMapping(String deviceId, Flux<DeviceMetadataMappingEntity> mappings) {

       return mappings
            .groupBy(e -> StringUtils.hasText(e.getOriginalId()))
            .flatMap(group -> {
                //bind
                if (group.key()) {
                    return deviceService
                        .findById(deviceId)
                        .flatMap(device -> this.save(
                            group.doOnNext(e -> {
                                e.setDeviceId(deviceId);
                                e.setProductId(device.getProductId());
                                e.generateId();
                                e.tryValidate(CreateGroup.class);
                            })
                        ));
                }
                //unbind
                return group
                    .map(mapping -> DeviceMetadataMappingEntity
                        .generateIdByDevice(deviceId, mapping.getMetadataType(), mapping.getMetadataId()))
                    .as(this::deleteById)
                    .then();
            })
           .then();
    }

    public Mono<Void> saveProductMapping(String productId, Flux<DeviceMetadataMappingEntity> mappings) {
        return mappings
            .groupBy(e -> StringUtils.hasText(e.getOriginalId()))
            .flatMap(group -> {
                //bind
                if (group.key()) {
                    return productService
                        .findById(productId)
                        .flatMap(device -> this.save(
                            group.doOnNext(e -> {
                                e.setDeviceId(null);
                                e.setProductId(productId);
                                e.generateId();
                                e.tryValidate(CreateGroup.class);
                            })
                        ));
                }
                //unbind
                return group
                    .map(mapping -> DeviceMetadataMappingEntity
                        .generateIdByProduct(productId, mapping.getMetadataType(), mapping.getMetadataId()))
                    .as(this::deleteById)
                    .then();
            })
            .then();
    }

    private Mono<DeviceMetadata> getProductMetadata(String productId) {
        //从数据库中获取物模型?
        return productService
            .findById(productId)
            .flatMap(product -> JetLinksDeviceMetadataCodec.getInstance().decode(product.getMetadata()));
    }

    private Mono<DeviceMetadata> getDeviceMetadata(DeviceInstanceEntity device) {
        if (StringUtils.hasText(device.getDeriveMetadata())) {
            return JetLinksDeviceMetadataCodec.getInstance().decode(device.getDeriveMetadata());
        }
        return getProductMetadata(device.getProductId());
    }

    private Flux<DeviceMetadataMappingDetail> convertDetail(ThingMetadata metadata,
                                                            Flux<DeviceMetadataMappingEntity> mappings,
                                                            Supplier<DeviceMetadataMappingDetail> builder) {

        return mappings
            .collect(Collectors.toMap(DeviceMetadataMappingEntity::getMetadataId,
                                      Function.identity(),
                                      //有设备ID则以设备配置的为准
                                      (left, right) -> StringUtils.hasText(left.getDeviceId()) ? left : right))
            .flatMapMany(mapping -> Flux
                .fromIterable(metadata.getProperties())
                .map(property -> builder
                    .get()
                    .with(property)
                    .with(mapping.get(property.getId()))));
    }

}
