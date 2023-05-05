package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.core.metadata.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@Slf4j
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
    public Flux<ConfigMetadata> getProductConfigMetadataByAccessId(String productId,
                                                                   String accessId) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier
                       .getProductConfigMetadataByAccessId(productId, accessId)
                       .onErrorResume(e -> {
                           log.error("get product config metatada by gateway error", e);
                           return Flux.empty();
                       }))
                   .map(config -> config.copy(DeviceConfigScope.product))
                   .filter(config -> !CollectionUtils.isEmpty(config.getProperties()))
                   .sort(Comparator.comparing(ConfigMetadata::getName));
    }

    @Override
    public Mono<Set<String>> getProductConfigMetadataProperties(String productId) {
        return this
            .getProductConfigMetadata(productId)
            .flatMapIterable(ConfigMetadata::getProperties)
            .map(ConfigPropertyMetadata::getProperty)
            .collect(Collectors.toSet());
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
        if (bean instanceof DeviceConfigMetadataSupplier) {
            register(((DeviceConfigMetadataSupplier) bean));
        }
        return bean;
    }

    @Override
    public Flux<Feature> getProductFeatures(String productId) {
        return Flux
            .fromIterable(suppliers)
            .flatMap(supplier -> supplier.getProductFeatures(productId))
            .distinct(Feature::getId);
    }
}
