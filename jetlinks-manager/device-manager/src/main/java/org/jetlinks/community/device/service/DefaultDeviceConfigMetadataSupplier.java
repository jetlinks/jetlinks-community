package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceConfigScope;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@AllArgsConstructor
public class DefaultDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {

    private final LocalDeviceInstanceService instanceService;

    private final LocalDeviceProductService productService;

    private final ProtocolSupports protocolSupports;

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {

        return instanceService
            .findById(deviceId)
            .map(DeviceInstanceEntity::getProductId)
            .flatMapMany(this::getProductConfigMetadata0)
            .filter(metadata -> metadata.hasScope(DeviceConfigScope.device));
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        return getProductConfigMetadata0(productId)
            .filter(metadata -> metadata.hasScope(DeviceConfigScope.product));
    }

    private Flux<ConfigMetadata> getProductConfigMetadata0(String productId) {
        return productService
            .findById(productId)
            .flatMapMany(product -> protocolSupports
                .getProtocol(product.getMessageProtocol())
                .flatMap(support -> support.getConfigMetadata(DefaultTransport.valueOf(product.getTransportProtocol()))));
    }
}
