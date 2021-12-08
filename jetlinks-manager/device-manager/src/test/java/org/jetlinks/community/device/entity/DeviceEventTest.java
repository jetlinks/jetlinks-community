package org.jetlinks.community.device.entity;

import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceEventTest {

  @Test
  void putFormat() {
      DeviceEvent deviceEvent = new DeviceEvent();

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

      EventMetadata fire_alarm = deviceProductOperator.getMetadata().map(s -> s.getEvent("fire_alarm").get()).block();
      deviceProductOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"string\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();
      EventMetadata fire_alarm1 = deviceProductOperator.getMetadata().map(s -> s.getEvent("fire_alarm").get()).block();


      deviceEvent.putFormat(null);
      deviceEvent.putFormat(fire_alarm);
      deviceEvent.putFormat(fire_alarm1);
      Map<String, Object> data = new HashMap<>();
      data.put("value",new HashMap<>());
      DeviceEvent deviceEvent1 = new DeviceEvent(data);
      deviceEvent1.putFormat(null);
  }
}