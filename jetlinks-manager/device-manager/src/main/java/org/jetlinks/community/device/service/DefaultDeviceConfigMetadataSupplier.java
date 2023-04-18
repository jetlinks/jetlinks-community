package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.community.gateway.supports.DeviceGatewayPropertiesManager;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.Transports;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceConfigScope;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.jetlinks.core.metadata.Feature;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;

@Component
@AllArgsConstructor
public class DefaultDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {

    private final LocalDeviceInstanceService instanceService;

    private final LocalDeviceProductService productService;

    private final ProtocolSupports protocolSupports;

    private final DeviceGatewayPropertiesManager gatewayPropertiesManager;

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

    public Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String typeId) {
        Assert.hasText(productId, "message.productId_cannot_be_empty");
        Assert.notNull(metadataType, "message.metadataType_cannot_be_empty");

        return this
            .computeDeviceProtocol(productId, (protocol, transport) ->
                protocol.getMetadataExpandsConfig(transport, metadataType, metadataId, typeId))
            .flatMapMany(Function.identity());
    }

    @Override
    public Flux<ConfigMetadata> getProductConfigMetadataByAccessId(String productId,
                                                                   String accessId) {
        return gatewayPropertiesManager
            .getProperties(accessId)
            .flatMapMany(properties -> protocolSupports
                .getProtocol(properties.getProtocol())
                .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, properties.getProtocol()))
                .flatMap(support -> support.getConfigMetadata(Transport.of(properties.getTransport()))));
    }

    private Flux<ConfigMetadata> getProductConfigMetadata0(String productId) {
        return productService
            .findById(productId)
            .filter(product -> StringUtils.hasText(product.getMessageProtocol()))
            .flatMapMany(product -> protocolSupports
                .getProtocol(product.getMessageProtocol())
                .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, product.getMessageProtocol()))
                .flatMap(support -> support.getConfigMetadata(Transport.of(product.getTransportProtocol()))));
    }

    @Override
    public Flux<Feature> getProductFeatures(String productId) {
        Assert.hasText(productId, "message.productId_cannot_be_empty");
        return this
            .computeDeviceProtocol(productId, ProtocolSupport::getFeatures)
            .flatMapMany(Function.identity());
    }


    @SuppressWarnings("all")
    protected <T> Mono<T> computeDeviceProtocol(String productId, BiFunction<ProtocolSupport, Transport, T> computer) {
        return productService
            .createQuery()
            .select(DeviceProductEntity::getMessageProtocol, DeviceProductEntity::getTransportProtocol)
            .where(DeviceInstanceEntity::getId, productId)
            .fetchOne()
            .flatMap(product -> {
                return Mono
                    .zip(
                        //消息协议
                        Mono.justOrEmpty(product.getMessageProtocol())
                            .flatMap(protocolSupports::getProtocol)
                            .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, product.getMessageProtocol())),
                        //传输协议
                        Mono.justOrEmpty(product.getTransportProtocol())
                            .map(Transport::of),
                        computer
                    );
            });
    }
}
