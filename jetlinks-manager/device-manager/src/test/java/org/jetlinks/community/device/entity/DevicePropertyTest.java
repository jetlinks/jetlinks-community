package org.jetlinks.community.device.entity;

import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.GeoPoint;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DevicePropertyTest {

    @Test
    void get() {
        DeviceProperty deviceProperty = new DeviceProperty();

        deviceProperty.setId("test");
        deviceProperty.setDeviceId("test");
        deviceProperty.setType("test");
        deviceProperty.setNumberValue("test");
        deviceProperty.setGeoValue(GeoPoint.of(1,0));
        deviceProperty.setFormatValue("test");
        deviceProperty.setFormatTime("test");
        deviceProperty.setCreateTime(10l);
        deviceProperty.setObjectValue("test");

        String id = deviceProperty.getId();
        assertNotNull(id);
        String deviceId = deviceProperty.getDeviceId();
        assertNotNull(deviceId);
        String type = deviceProperty.getType();
        assertNotNull(type);
        Object numberValue = deviceProperty.getNumberValue();
        assertNotNull(numberValue);
        GeoPoint geoPoint = deviceProperty.getGeoValue();
        assertNotNull(geoPoint);
        String formatTime = deviceProperty.getFormatTime();
        assertNotNull(formatTime);
        Object formatValue = deviceProperty.getFormatValue();
        assertNotNull(formatValue);
        long createTime = deviceProperty.getCreateTime();
        assertNotNull(createTime);
        Object objectValue = deviceProperty.getObjectValue();
        assertNotNull(objectValue);
    }

    @Test
    void formatTime(){
        DeviceProperty property = new DeviceProperty();
        DeviceProperty test = property.formatTime("test");
        assertNotNull(test);
    }
    @Test
    void withProperty(){
        DeviceProperty property = new DeviceProperty();

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId("test");
        deviceProductEntity.setTransportProtocol("TCP");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map = new HashMap<>();
        map.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"object\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        PropertyMetadata temperature = deviceProductOperator.getMetadata().map(s -> s.getProperty("temperature").get()).block();

        property.withProperty(temperature);

        DeviceProperty.of("test",temperature);

        DeviceTagEntity test = DeviceTagEntity.of(temperature, "test");

    }

    @Test
    void deviceSate(){
        DeviceStateInfo test = DeviceStateInfo.of("test", DeviceState.online);
        String deviceId = test.getDeviceId();
        assertNotNull(deviceId);
    }

    @Test
    void deviceTag(){

    }

}