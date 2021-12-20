package org.jetlinks.community.device.measurements;

import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceDynamicDashboardTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void init() {
    }

    @Test
    void getObjects() {
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
        ReactiveQuery<DeviceProductEntity> query = Mockito.mock(ReactiveQuery.class);

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setTransportProtocol("TCP");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map = new HashMap<>();
        map.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();

        Mockito.when(productService.createQuery())
            .thenReturn(query);
        Mockito.when(query.fetch())
            .thenReturn(Flux.just(deviceProductEntity));

        Mockito.when(registry.getProduct(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductOperator));

        DeviceDynamicDashboard dashboard = new DeviceDynamicDashboard(productService, registry, dataService, new BrokerEventBus());
        dashboard.getObjects().subscribe();

        Mockito.when(productService.findById(Mockito.anyString()))
            .thenReturn(Mono.just(deviceProductEntity));
        dashboard.getObject(PRODUCT_ID).subscribe();
    }

    @Test
    void getDefinition(){
        LocalDeviceProductService productService = Mockito.mock(LocalDeviceProductService.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        DeviceDataService dataService = Mockito.mock(DeviceDataService.class);

        DeviceDashboard dashboard = new DeviceDynamicDashboard(productService, registry, dataService, new BrokerEventBus());
        DashboardDefinition definition = dashboard.getDefinition();
        String id = definition.getId();
        assertNotNull(id);
        String name = definition.getName();
        assertNotNull(name);
    }


}