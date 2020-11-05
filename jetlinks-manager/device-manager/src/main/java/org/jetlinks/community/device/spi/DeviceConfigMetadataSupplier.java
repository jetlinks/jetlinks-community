package org.jetlinks.community.device.spi;

import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Flux;

public interface DeviceConfigMetadataSupplier {

    Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId);

    Flux<ConfigMetadata> getProductConfigMetadata(String productId);

}
