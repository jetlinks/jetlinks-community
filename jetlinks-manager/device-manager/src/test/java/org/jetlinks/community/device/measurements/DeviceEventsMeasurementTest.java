package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.SimpleMeasurementValue;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceEventsMeasurementTest {
    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";
  @Test
  void fromHistory() {
      DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
      DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
      deviceInstanceEntity.setId(DEVICE_ID);
      deviceInstanceEntity.setState(DeviceState.online);
      deviceInstanceEntity.setCreatorName("超级管理员");
      deviceInstanceEntity.setName("TCP-setvice");
      deviceInstanceEntity.setProductId(PRODUCT_ID);
      deviceInstanceEntity.setProductName("TCP测试");
      deviceInstanceEntity.setDeriveMetadata(
          "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

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
      DeviceOperator deviceOperator = InMemoryDeviceRegistry.create().register(deviceInstanceEntity.toDeviceInfo()).block();
      DeviceMetadata deviceMetadata = deviceProductOperator.getMetadata().block();

      DeviceEventsMeasurement measurement = new DeviceEventsMeasurement(DEVICE_ID,new BrokerEventBus(),deviceMetadata,dataService);
      measurement.fromHistory(DEVICE_ID,0).subscribe();

      DeviceEvent deviceEvent = new DeviceEvent();
      Mockito.when(dataService.queryEvent(Mockito.anyString(),Mockito.anyString(),Mockito.any(QueryParamEntity.class),Mockito.anyBoolean()))
          .thenReturn(Flux.just(deviceEvent));
      measurement.fromHistory(DEVICE_ID,1)
          .map(SimpleMeasurementValue::getTimestamp)
          .as(StepVerifier::create)
          .expectNext(0L)
          .verifyComplete();


  }

  @Test
  void createValue() {
  }

  @Test
  void fromRealTime() {
  }
}