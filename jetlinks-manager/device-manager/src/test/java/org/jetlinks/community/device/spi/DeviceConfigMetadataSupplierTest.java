package org.jetlinks.community.device.spi;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadataType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceConfigMetadataSupplierTest {

    @Test
    void getMetadataExpandsConfig1(){
        class TestDeviceConfigMetadataSupplier implements DeviceConfigMetadataSupplier {

            @Override
            public Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
                return null;
            }

            @Override
            public Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId) {
                return null;
            }

            @Override
            public Flux<ConfigMetadata> getProductConfigMetadata(String productId) {
                return null;
            }
        }
        DeviceConfigMetadataSupplier testDeviceConfigMetadataSupplier = new TestDeviceConfigMetadataSupplier();
        assertNotNull(testDeviceConfigMetadataSupplier);
        testDeviceConfigMetadataSupplier.getMetadataExpandsConfig("PRODUCT_ID", DeviceMetadataType.property,"","")
            .map(ConfigMetadata::getName)
            .as(StepVerifier::create)
            .expectComplete()
            .verify();
    }
}