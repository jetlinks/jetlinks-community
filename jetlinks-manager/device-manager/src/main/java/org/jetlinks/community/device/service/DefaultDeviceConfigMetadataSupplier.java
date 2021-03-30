package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.Transports;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceConfigScope;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.function.Function;

@Component
@AllArgsConstructor
public class DefaultDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {

    private final LocalDeviceInstanceService instanceService;

    private final LocalDeviceProductService productService;

    private final ProtocolSupports protocolSupports;

    @Override
    @SuppressWarnings("all")
    public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        if(StringUtils.isEmpty(deviceId)){
            return Flux.empty();
        }
        return instanceService
            .createQuery()
            .select(DeviceInstanceEntity::getProductId)
            .where(DeviceInstanceEntity::getId,deviceId)
            .fetchOne()
            .map(DeviceInstanceEntity::getProductId)
            .flatMapMany(this::getProductConfigMetadata0)
            .filter(metadata -> metadata.hasScope(DeviceConfigScope.device));
    }

    @Override
    public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
        if(StringUtils.isEmpty(productId)){
            return Flux.empty();
        }
        return getProductConfigMetadata0(productId)
            .filter(metadata -> metadata.hasScope(DeviceConfigScope.device));
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
        if(StringUtils.isEmpty(productId)){
            return Flux.empty();
        }
        return getProductConfigMetadata0(productId)
            .filter(metadata -> metadata.hasScope(DeviceConfigScope.product));
    }

    @Override
    @SuppressWarnings("all")
    public Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String typeId) {
        Assert.hasText(productId, "productId can not be empty");
        Assert.notNull(metadataType, "metadataType can not be empty");

        return productService
            .createQuery()
            .select(DeviceProductEntity::getMessageProtocol, DeviceProductEntity::getTransportProtocol)
            .where(DeviceInstanceEntity::getId, productId)
            .fetchOne()
            .flatMap(product -> {
                return Mono
                    .zip(
                        //消息协议
                        protocolSupports.getProtocol(product.getMessageProtocol()),
                        //传输协议
                        Mono.justOrEmpty(product.getTransportEnum(Transports.get())),
                        (protocol, transport) -> {
                            return protocol.getMetadataExpandsConfig(transport, metadataType, metadataId, typeId);
                        }
                    );
            })
            .flatMapMany(Function.identity());
    }

    private Flux<ConfigMetadata> getProductConfigMetadata0(String productId) {
        return productService
            .findById(productId)
            .flatMapMany(product -> protocolSupports
                .getProtocol(product.getMessageProtocol())
                .flatMap(support -> support.getConfigMetadata(Transport.of(product.getTransportProtocol()))));
    }
}
