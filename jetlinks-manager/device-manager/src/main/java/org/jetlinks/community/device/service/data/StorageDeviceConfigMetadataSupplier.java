package org.jetlinks.community.device.service.data;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Value;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategies;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class StorageDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {
    private final DeviceRegistry registry;

    private final DeviceDataStorageProperties properties;

    private final ConfigMetadata objectConf = new DefaultConfigMetadata("存储配置", "")
        .scope(DeviceConfigScope.product)
        .add(StorageConstants.propertyStorageType, "存储方式", new EnumType()
            .addElement(EnumType.Element.of("direct", "直接存储", "直接存储上报的数据"))
            .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeIgnore, "不存储", "不存储此属性值"))
            .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeJson, "JSON字符", "将数据序列话为JSON字符串进行存储"))
        );

    private final ConfigMetadata anotherConf = new DefaultConfigMetadata("存储配置", "")
        .scope(DeviceConfigScope.product)
        .add(StorageConstants.propertyStorageType, "存储方式", new EnumType()
            .addElement(EnumType.Element.of("direct", "存储", "将上报的属性值保存到配置到存储策略中"))
            .addElement(EnumType.Element.of(StorageConstants.propertyStorageTypeIgnore, "不存储", "不存储此属性值"))
        );


    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return Flux.empty();
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
        return Flux.empty();
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return Flux.empty();
    }

    @Override
    public Flux<Feature> getProductFeatures(String productId) {
        return registry
            .getProduct(productId)
            .flatMap(prod -> prod.getConfig(DeviceDataService.STORE_POLICY_CONFIG_KEY))
            .defaultIfEmpty(properties.getDefaultPolicy())
            .flatMap(this::getStoragePolicy)
            .flatMapMany(strategy -> strategy
                .opsForSave(ThingsDataRepositoryStrategy.OperationsContext.DEFAULT)
                .getFeatures());
    }

    private Mono<ThingsDataRepositoryStrategy> getStoragePolicy(String policy) {
        return Mono.justOrEmpty(ThingsDataRepositoryStrategies.getStrategy(policy));
    }

    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String typeId) {
        if (metadataType == DeviceMetadataType.property) {
            if ((ObjectType.ID.equals(typeId) || ArrayType.ID.equals(typeId))) {
                return registry
                    .getProduct(productId)
                    .flatMap(prod -> prod
                        .getConfig(StorageConstants.storePolicyConfigKey)
                        .map(Value::asString))
                    .defaultIfEmpty(properties.getDefaultPolicy())
                    .filter(policy -> policy.startsWith("default-"))
                    .map(ignore -> objectConf)
                    .flux();
            }
            return Flux.just(anotherConf);
        }

        return Flux.empty();

    }
}
