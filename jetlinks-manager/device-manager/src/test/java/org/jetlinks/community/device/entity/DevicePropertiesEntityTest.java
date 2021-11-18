package org.jetlinks.community.device.entity;

import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DevicePropertiesEntityTest {

    @Test
    void get() {
        DevicePropertiesEntity entity = new DevicePropertiesEntity();
        entity.setId("test");
        entity.setDeviceId("test");
        entity.setProperty("test");
        entity.setPropertyName("test");
        entity.setStringValue("test");
        entity.setFormatValue("test");
        entity.setGeoValue(GeoPoint.of(1, 0));
        entity.setTimestamp(10l);
        entity.setObjectValue("test");
        entity.setValue("test");
        entity.setOrgId("test");
        entity.setProductId("test");
        entity.setTimeValue(new Date());
        entity.setType("test");
        entity.setCreateTime(10l);

        String id = entity.getId();
        assertNotNull(id);
        String deviceId = entity.getDeviceId();
        assertNotNull(deviceId);
        String property = entity.getProperty();
        assertNotNull(property);
        String propertyName = entity.getPropertyName();
        assertNotNull(propertyName);
        String stringValue = entity.getStringValue();
        assertNotNull(stringValue);
        String formatValue = entity.getFormatValue();
        assertNotNull(formatValue);
        GeoPoint geoPoint = entity.getGeoValue();
        assertNotNull(geoPoint);
        long timestamp = entity.getTimestamp();
        assertNotNull(timestamp);
        Object objectValue = entity.getObjectValue();
        assertNotNull(objectValue);
        String value = entity.getValue();
        assertNotNull(value);
        String orgId = entity.getOrgId();
        assertNotNull(orgId);
        String productId = entity.getProductId();
        assertNotNull(productId);
        Date timeValue = entity.getTimeValue();
        assertNotNull(timeValue);
        String type = entity.getType();
        assertNotNull(type);
        long createTime = entity.getCreateTime();
        assertNotNull(createTime);

        DevicePropertiesEntity.builder().id("test").build();
    }

    @Test
    void toMap() {
        DevicePropertiesEntity entity = new DevicePropertiesEntity();
        Map<String, Object> map = entity.toMap();
        assertNotNull(map);
    }

    @Test
    void withValue() {
        DevicePropertiesEntity entity = new DevicePropertiesEntity();
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
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        InMemoryDeviceRegistry inMemoryDeviceRegistry = InMemoryDeviceRegistry.create();
        DeviceProductOperator deviceProductOperator = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        PropertyMetadata temperature = deviceProductOperator.getMetadata().map(s -> s.getProperty("temperature").get()).block();

        entity.withValue(temperature, 12);
        entity.withValue(temperature, null);
        Executable executable = ()->entity.withValue(temperature, "test");
        assertThrows(UnsupportedOperationException.class,executable,"无法将test转为float");

        entity.withValue(temperature,new BigDecimal(1));
        PropertyMetadata propertyMetadata = null;
        entity.withValue(propertyMetadata, 12);
        entity.withValue(propertyMetadata,new Date());
    }

    @Test
    void withValue1(){
        DevicePropertiesEntity entity = new DevicePropertiesEntity();

        entity.withValue(new IntType(),12);
        entity.withValue(new LongType(),12);
        entity.withValue(DoubleType.GLOBAL,12.12);
        entity.withValue(BigDecimalType.GLOBAL,12);

        entity.withValue(new DateTimeType(), LocalDateTime.now());

        entity.withValue(new ObjectType(),new HashMap<>());
        entity.withValue(new ArrayType(),new String[]{"test","test1"});
        entity.withValue(new GeoType(),GeoPoint.of(1,1));
        entity.withValue(new StringType(),"test");

    }
}