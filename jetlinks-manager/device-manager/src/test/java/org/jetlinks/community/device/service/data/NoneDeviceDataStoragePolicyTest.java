package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;


class NoneDeviceDataStoragePolicyTest {

    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void saveDeviceMessage(){
        new NoneDeviceDataStoragePolicy()
            .saveDeviceMessage(new DeviceOnlineMessage())
            .subscribe();
    }
    @Test
    void saveDeviceMessage1(){
        new NoneDeviceDataStoragePolicy()
            .saveDeviceMessage(Mono.just(new DeviceOnlineMessage()))
            .subscribe();
    }
    @Test
    void registerMetadata() {
        new NoneDeviceDataStoragePolicy()
            .registerMetadata(PRODUCT_ID, new JetLinksDeviceMetadata("test", "test"))
            .subscribe();
    }

    @Test
    void queryEachOneProperties() {
        new NoneDeviceDataStoragePolicy()
            .queryEachOneProperties(DEVICE_ID, new QueryParamEntity(), "test")
            .subscribe();
    }

    @Test
    void queryEvent() {
        new NoneDeviceDataStoragePolicy()
            .queryEvent(DEVICE_ID, "event", new QueryParamEntity(), true)
            .subscribe();
    }

    @Test
    void queryEventPage() {
        new NoneDeviceDataStoragePolicy()
            .queryEventPage(DEVICE_ID, "event", new QueryParamEntity(), true)
            .subscribe();

    }

    @Test
    void queryEachProperties() {
        new NoneDeviceDataStoragePolicy()
            .queryEachProperties(DEVICE_ID, new QueryParamEntity(), "test")
            .subscribe();
    }

    @Test
    void queryProperty() {
        new NoneDeviceDataStoragePolicy()
            .queryProperty(DEVICE_ID, new QueryParamEntity(), "test")
            .subscribe();
    }

    @Test
    void aggregationPropertiesByProduct() {
        new NoneDeviceDataStoragePolicy()
            .aggregationPropertiesByProduct(PRODUCT_ID, new DeviceDataService.AggregationRequest(), new DeviceDataService.DevicePropertyAggregation())
            .subscribe();
    }

    @Test
    void aggregationPropertiesByDevice() {
        new NoneDeviceDataStoragePolicy()
            .aggregationPropertiesByDevice(DEVICE_ID, new DeviceDataService.AggregationRequest(), new DeviceDataService.DevicePropertyAggregation())
            .subscribe();
    }

    @Test
    void queryPropertyPage() {
        new NoneDeviceDataStoragePolicy()
            .queryPropertyPage(DEVICE_ID, "test", new QueryParamEntity())
            .subscribe();
    }

    @Test
    void queryDeviceMessageLog() {
        new NoneDeviceDataStoragePolicy()
            .queryDeviceMessageLog(DEVICE_ID, new QueryParamEntity())
            .subscribe();
    }
}