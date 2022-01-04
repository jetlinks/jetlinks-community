package org.jetlinks.community.device.service;

import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDeviceConfigMetadataManagerTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void register() {
        DefaultDeviceConfigMetadataSupplier supplier = Mockito.mock(DefaultDeviceConfigMetadataSupplier.class);

        DefaultDeviceConfigMetadataManager service = new DefaultDeviceConfigMetadataManager();
        assertNotNull(service);
        service.register(supplier);
    }

    @Test
    void getDeviceConfigMetadataByProductId() {
        DefaultDeviceConfigMetadataSupplier supplier = Mockito.mock(DefaultDeviceConfigMetadataSupplier.class);

        DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
        defaultConfigMetadata.add("test", "1", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata.setName("a");
        DefaultConfigMetadata defaultConfigMetadata3 = new DefaultConfigMetadata();
        defaultConfigMetadata3.add("test", "3", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata3.setName("c");
        DefaultConfigMetadata defaultConfigMetadata2 = new DefaultConfigMetadata();
        defaultConfigMetadata2.add("test", "2", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata2.setName("b");
        DefaultConfigMetadata defaultConfigMetadata1 = new DefaultConfigMetadata();
        defaultConfigMetadata1.setName("d");

        Mockito.when(supplier.getDeviceConfigMetadataByProductId(Mockito.anyString()))
            .thenReturn(Flux.just(defaultConfigMetadata,defaultConfigMetadata1,defaultConfigMetadata2,defaultConfigMetadata3));

        DefaultDeviceConfigMetadataManager service = new DefaultDeviceConfigMetadataManager();
        assertNotNull(service);
        service.register(supplier);
        service.getDeviceConfigMetadataByProductId(PRODUCT_ID)
            .map(ConfigMetadata::getProperties)
            .map(s->s.get(0))
            .map(ConfigPropertyMetadata::getName)
            .as(StepVerifier::create)
            .expectNext("1")
            .expectNext("2")
            .expectNext("3")
            .verifyComplete();

    }

    @Test
    void getDeviceConfigMetadata() {
        DefaultDeviceConfigMetadataSupplier supplier = Mockito.mock(DefaultDeviceConfigMetadataSupplier.class);

        DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
        defaultConfigMetadata.add("test", "1", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata.setName("a");
        DefaultConfigMetadata defaultConfigMetadata3 = new DefaultConfigMetadata();
        defaultConfigMetadata3.add("test", "3", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata3.setName("c");
        DefaultConfigMetadata defaultConfigMetadata2 = new DefaultConfigMetadata();
        defaultConfigMetadata2.add("test", "2", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata2.setName("b");
        DefaultConfigMetadata defaultConfigMetadata1 = new DefaultConfigMetadata();
        defaultConfigMetadata1.setName("d");

        Mockito.when(supplier.getDeviceConfigMetadata(Mockito.anyString()))
            .thenReturn(Flux.just(defaultConfigMetadata,defaultConfigMetadata1,defaultConfigMetadata2,defaultConfigMetadata3));

        DefaultDeviceConfigMetadataManager service = new DefaultDeviceConfigMetadataManager();
        assertNotNull(service);
        service.register(supplier);
        service.getDeviceConfigMetadata(DEVICE_ID)
            .map(ConfigMetadata::getProperties)
            .map(s->s.get(0))
            .map(ConfigPropertyMetadata::getName)
            .as(StepVerifier::create)
            .expectNext("1")
            .expectNext("2")
            .expectNext("3")
            .verifyComplete();
    }

    @Test
    void getProductConfigMetadata() {
        DefaultDeviceConfigMetadataSupplier supplier = Mockito.mock(DefaultDeviceConfigMetadataSupplier.class);

        DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
        defaultConfigMetadata.add("test", "1", StringType.GLOBAL, DeviceConfigScope.product);
        defaultConfigMetadata.setName("a");
        DefaultConfigMetadata defaultConfigMetadata3 = new DefaultConfigMetadata();
        defaultConfigMetadata3.add("test", "3", StringType.GLOBAL, DeviceConfigScope.product);
        defaultConfigMetadata3.setName("c");
        DefaultConfigMetadata defaultConfigMetadata2 = new DefaultConfigMetadata();
        defaultConfigMetadata2.add("test", "2", StringType.GLOBAL, DeviceConfigScope.product);
        defaultConfigMetadata2.setName("b");
        DefaultConfigMetadata defaultConfigMetadata1 = new DefaultConfigMetadata();
        defaultConfigMetadata1.setName("d");
        DefaultConfigMetadata defaultConfigMetadata4 = new DefaultConfigMetadata();
        defaultConfigMetadata4.setName("e");
        defaultConfigMetadata4.add("test", "2", StringType.GLOBAL, DeviceConfigScope.device);

        Mockito.when(supplier.getProductConfigMetadata(Mockito.anyString()))
            .thenReturn(Flux.just(defaultConfigMetadata,defaultConfigMetadata1,defaultConfigMetadata2,defaultConfigMetadata3,defaultConfigMetadata4));

        DefaultDeviceConfigMetadataManager service = new DefaultDeviceConfigMetadataManager();
        assertNotNull(service);
        service.register(supplier);
        service.getProductConfigMetadata(PRODUCT_ID)
            .map(ConfigMetadata::getProperties)
            .map(s->s.get(0))
            .map(ConfigPropertyMetadata::getName)
            .as(StepVerifier::create)
            .expectNext("1")
            .expectNext("2")
            .expectNext("3")
            .verifyComplete();
    }

    @Test
    void getMetadataExpandsConfig() {
        DefaultDeviceConfigMetadataSupplier supplier = Mockito.mock(DefaultDeviceConfigMetadataSupplier.class);

        DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();
        defaultConfigMetadata.add("test", "1", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata.setName("a");
        DefaultConfigMetadata defaultConfigMetadata3 = new DefaultConfigMetadata();
        defaultConfigMetadata3.add("test", "3", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata3.setName("c");
        DefaultConfigMetadata defaultConfigMetadata2 = new DefaultConfigMetadata();
        defaultConfigMetadata2.add("test", "2", StringType.GLOBAL, DeviceConfigScope.device);
        defaultConfigMetadata2.setName("b");
        DefaultConfigMetadata defaultConfigMetadata1 = new DefaultConfigMetadata();
        defaultConfigMetadata1.setName("d");
        DefaultConfigMetadata defaultConfigMetadata4 = new DefaultConfigMetadata();
        defaultConfigMetadata4.setName("e");
        defaultConfigMetadata4.add("test", "2", StringType.GLOBAL, DeviceConfigScope.product);
        Mockito.when(supplier.getMetadataExpandsConfig(Mockito.anyString(),Mockito.any(DeviceMetadataType.class),Mockito.anyString(),Mockito.anyString()))
            .thenReturn(Flux.just(defaultConfigMetadata,defaultConfigMetadata1,defaultConfigMetadata2,defaultConfigMetadata3,defaultConfigMetadata4));

        DefaultDeviceConfigMetadataManager service = new DefaultDeviceConfigMetadataManager();
        assertNotNull(service);
        service.register(supplier);
        service.getMetadataExpandsConfig(PRODUCT_ID, DeviceMetadataType.property,"test","test", DeviceConfigScope.device)
            .map(ConfigMetadata::getProperties)
            .map(s->s.get(0))
            .map(ConfigPropertyMetadata::getName)
            .as(StepVerifier::create)
            .expectNext("1")
            .expectNext("2")
            .expectNext("3")
            .verifyComplete();
    }

    @Test
    void postProcessAfterInitialization() {
        DefaultDeviceConfigMetadataManager service = new DefaultDeviceConfigMetadataManager();
        DefaultDeviceConfigMetadataSupplier supplier = Mockito.mock(DefaultDeviceConfigMetadataSupplier.class);
        Object defaultDeviceConfigMetadataSupplier = service.postProcessAfterInitialization(supplier, "defaultDeviceConfigMetadataSupplier");
        assertTrue(defaultDeviceConfigMetadataSupplier instanceof DeviceConfigMetadataSupplier);
    }
}