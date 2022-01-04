package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoneDeviceDataStoragePolicyTest {

    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void saveDeviceMessage() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.saveDeviceMessage(new DeviceOnlineMessage())
            .subscribe();
    }

    @Test
    void saveDeviceMessage1() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.saveDeviceMessage(Mono.just(new DeviceOnlineMessage()))
            .subscribe();
    }

    @Test
    void registerMetadata() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.registerMetadata(PRODUCT_ID, new JetLinksDeviceMetadata("test", "test"))
            .subscribe();
    }

    @Test
    void queryEachOneProperties() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryEachOneProperties(DEVICE_ID, new QueryParamEntity(), "test")
            .subscribe();
    }

    @Test
    void queryEvent() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryEvent(DEVICE_ID, "event", new QueryParamEntity(), true)
            .subscribe();
    }

    @Test
    void queryEventPage() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryEventPage(DEVICE_ID, "event", new QueryParamEntity(), true)
            .subscribe();

    }

    @Test
    void queryEachProperties() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryEachProperties(DEVICE_ID, new QueryParamEntity(), "test")
            .subscribe();
    }

    @Test
    void queryProperty() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryProperty(DEVICE_ID, new QueryParamEntity(), "test")
            .subscribe();
    }

    @Test
    void aggregationPropertiesByProduct() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.aggregationPropertiesByProduct(PRODUCT_ID, new DeviceDataService.AggregationRequest(), new DeviceDataService.DevicePropertyAggregation())
            .subscribe();
    }

    @Test
    void aggregationPropertiesByDevice() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.aggregationPropertiesByDevice(DEVICE_ID, new DeviceDataService.AggregationRequest(), new DeviceDataService.DevicePropertyAggregation())
            .subscribe();
    }

    @Test
    void queryPropertyPage() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryPropertyPage(DEVICE_ID, "test", new QueryParamEntity())
            .subscribe();
    }

    @Test
    void queryDeviceMessageLog() {
        NoneDeviceDataStoragePolicy noneDeviceDataStoragePolicy = new NoneDeviceDataStoragePolicy();
        assertNotNull(noneDeviceDataStoragePolicy);
        noneDeviceDataStoragePolicy.queryDeviceMessageLog(DEVICE_ID, new QueryParamEntity())
            .subscribe();
    }
}