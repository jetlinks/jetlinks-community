package org.jetlinks.community.plugin.device;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.*;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ExternalDeviceRegistry implements DeviceRegistry {

    private final String pluginId;

    private final PluginDataIdMapper idMapper;

    private final DeviceRegistry internal;

    @Override
    public Mono<DeviceOperator> getDevice(String deviceId) {
        return idMapper
            .getInternalId(PluginDataIdMapper.TYPE_DEVICE, pluginId, deviceId)
            .flatMap(internal::getDevice)
            .map(opt -> new ExternalDeviceOperator(deviceId, pluginId, idMapper, opt));
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId) {
        return idMapper
            .getInternalId(PluginDataIdMapper.TYPE_PRODUCT, pluginId, productId)
            .flatMap(internal::getProduct)
            .map(opt -> new ExternalDeviceProductOperator(productId, opt));
    }

    @Override
    public Mono<DeviceOperator> register(DeviceInfo deviceInfo) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<DeviceProductOperator> register(ProductInfo productInfo) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<Void> unregisterDevice(String deviceId) {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    public Mono<Void> unregisterProduct(String productId) {
        return Mono.error(new UnsupportedOperationException());
    }
}
