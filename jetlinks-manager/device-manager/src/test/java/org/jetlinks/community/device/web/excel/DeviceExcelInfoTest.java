package org.jetlinks.community.device.web.excel;

import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceExcelInfoTest {

    @Test
    void config() {
        DeviceExcelInfo excelInfo = new DeviceExcelInfo();
        excelInfo.setId("test");
        excelInfo.config("test","test");

        excelInfo.tag("test","test",null,"string");
        excelInfo.tag("test","test","test","string");

        excelInfo.setId("test");

        excelInfo.toMap();
    }

    @Test
    void getTemplateHeaderMapping(){

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
        //PropertyMetadata temperature = deviceProductOperator.getMetadata().map(s -> s.getProperty("temperature").get()).block();
        List<PropertyMetadata> tags = deviceProductOperator.getMetadata().map(DeviceMetadata::getTags).block();
        DeviceExcelInfo.getTemplateHeaderMapping(tags,new ArrayList<>());

        DeviceExcelInfo.getExportHeaderMapping(tags,new ArrayList<>());


    }
}