package org.jetlinks.community.device.service;

import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
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
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier.getDeviceConfigMetadata(deviceId))
                   .sort(Comparator.comparing(ConfigMetadata::getName));
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return Flux.fromIterable(suppliers)
                   .flatMap(supplier -> supplier.getProductConfigMetadata(productId))
                   .sort(Comparator.comparing(ConfigMetadata::getName));
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
        if (bean instanceof DeviceConfigMetadataSupplier) {
            register(((DeviceConfigMetadataSupplier) bean));
        }
        return bean;
    }
}
