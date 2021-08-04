package org.jetlinks.community.device.service;

import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.ConfigScope;
import org.jetlinks.core.metadata.DeviceConfigScope;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DefaultDeviceConfigMetadataManager implements DeviceConfigMetadataManager, BeanPostProcessor {

    private final List<DeviceConfigMetadataSupplier> suppliers = new CopyOnWriteArrayList<>();

    protected void register(DeviceConfigMetadataSupplier supplier) {
        suppliers.add(supplier);
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier.getDeviceConfigMetadataByProductId(productId))
                   .map(config -> config.copy(DeviceConfigScope.device))
                   .filter(config-> !CollectionUtils.isEmpty(config.getProperties()))
                   .sort(Comparator.comparing(ConfigMetadata::getName));
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier.getDeviceConfigMetadata(deviceId))
                   .map(config -> config.copy(DeviceConfigScope.device))
                   .filter(config-> !CollectionUtils.isEmpty(config.getProperties()))
                   .sort(Comparator.comparing(ConfigMetadata::getName));
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier.getProductConfigMetadata(productId))
                   .map(config -> config.copy(DeviceConfigScope.product))
                   .filter(config-> !CollectionUtils.isEmpty(config.getProperties()))
                   .sort(Comparator.comparing(ConfigMetadata::getName));
    }

    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String typeId,
                                                         ConfigScope... scopes) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier.getMetadataExpandsConfig(productId, metadataType, metadataId, typeId))
                   .sort(Comparator.comparing(ConfigMetadata::getName))
                   .filter(metadata -> metadata.hasAnyScope(scopes))
                   .map(metadata -> metadata.copy(scopes))
                   .filter(meta -> org.apache.commons.collections4.CollectionUtils.isNotEmpty(meta.getProperties()));
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
        if (bean instanceof DeviceConfigMetadataSupplier) {
            register(((DeviceConfigMetadataSupplier) bean));
        }
        return bean;
    }
}
