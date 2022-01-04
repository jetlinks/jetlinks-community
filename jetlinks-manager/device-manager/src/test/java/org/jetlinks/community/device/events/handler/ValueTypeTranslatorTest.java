package org.jetlinks.community.device.events.handler;


import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValueTypeTranslatorTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void translator() {
        Object d = ValueTypeTranslator.translator("12.12", DoubleType.GLOBAL);
        assertNotNull(d);
        Object f = ValueTypeTranslator.translator("12.12", FloatType.GLOBAL);
        assertNotNull(f);
        Object l = ValueTypeTranslator.translator("12", LongType.GLOBAL);
        assertNotNull(l);
        Object b = ValueTypeTranslator.translator("true", BooleanType.GLOBAL);
        assertNotNull(b);
        Object i = ValueTypeTranslator.translator("12", IntType.GLOBAL);
        assertNotNull(i);
        Object date = ValueTypeTranslator.translator(LocalDateTime.now(), DateTimeType.GLOBAL);
        assertNotNull(date);
        Object test = ValueTypeTranslator.translator("test", StringType.GLOBAL);
        assertNotNull(test);

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
        DeviceMetadata deviceMetadata = deviceProductOperator.getMetadata().block();

        ObjectType objectType = new ObjectType();
        objectType.setProperties(deviceMetadata.getProperties());
        Map<String, Object> map1 = new HashMap<>();
        map1.put("temperature","temperature");
        Object obj = ValueTypeTranslator.translator(map1, objectType);
        assertNotNull(obj);


        Object o = ValueTypeTranslator.translator("sss", DateTimeType.GLOBAL);
        assertNotNull(o);


    }
}