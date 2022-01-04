package org.jetlinks.community.device.web;


import com.google.common.cache.Cache;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.StandaloneDeviceMessageBroker;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetlinks.core.device.DeviceConfigKey.connectionServerId;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@WebFluxTest(DeviceMessageController.class)
class DeviceMessageControllerTest extends TestJetLinksController {

    @Autowired
    private ClusterDeviceRegistry clusterDeviceRegistry;

    public static final String BASE_URL = "/device";
    public static final String DEVICE_ID = "10000";
    public static final String PRODUCT_ID = "1236859833832701954";

    void add(DeviceMessageReply message) {
        assertNotNull(client);
        assertNotNull(clusterDeviceRegistry);
        Class<? extends ClusterDeviceRegistry> aClass = clusterDeviceRegistry.getClass();
        Field operatorCache = null;
        try {
            operatorCache = aClass.getDeclaredField("operatorCache");
        } catch (Exception e) {
            e.printStackTrace();
        }
        operatorCache.setAccessible(true);
        Cache<String, Mono<DeviceOperator>> cache = null;
        try {
            cache = (Cache<String, Mono<DeviceOperator>>) operatorCache.get(clusterDeviceRegistry);
        } catch (Exception e) {
            e.printStackTrace();
        }

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


        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(message));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);
        deviceOperator.setConfig(connectionServerId.getKey(),"test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(), "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();


        cache.put(DEVICE_ID,Mono.just(deviceOperator));

        System.out.println(cache.size());
    }

    @Test
    void getProperty() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        add(readPropertyMessageReply);
        client.get()
            .uri(BASE_URL+"/"+DEVICE_ID+"/property/temperature")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
    @Test
    void getProperty1() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        readPropertyMessageReply.setCode(ErrorCode.REQUEST_HANDLING.name());
        readPropertyMessageReply.setMessage("request_handling");
        add(readPropertyMessageReply);
        client.get()
            .uri(BASE_URL+"/"+DEVICE_ID+"/property/temperature")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }

    @Test
    void getProperty2() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        readPropertyMessageReply.setSuccess(false);
        readPropertyMessageReply.setMessage("发送失败");
        add(readPropertyMessageReply);
        client.get()
            .uri(BASE_URL+"/"+DEVICE_ID+"/property/temperature")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }
    @Test
    void getProperty3() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(null);
        add(readPropertyMessageReply);
        client.get()
            .uri(BASE_URL+"/"+DEVICE_ID+"/property/temperature")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }

    @Test
    void getStandardProperty() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        add(readPropertyMessageReply);
        client.get()
            .uri(BASE_URL+"/standard/"+DEVICE_ID+"/property/temperature")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void settingProperties() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        WritePropertyMessageReply writePropertyMessageReply = WritePropertyMessageReply.create();
        writePropertyMessageReply.setProperties(hashMap);
        add(writePropertyMessageReply);
        Map<String, Object> properties = new HashMap<>();
        properties.put("test","test");
        client.post()
            .uri(BASE_URL+"/setting/"+DEVICE_ID+"/property")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(properties)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void invokedFunction() {
        assertNotNull(client);
        FunctionInvokeMessageReply functionInvokeMessageReply = new FunctionInvokeMessageReply();
        functionInvokeMessageReply.setOutput("test");
        add(functionInvokeMessageReply);
        Map<String, Object> properties = new HashMap<>();
        properties.put("outTime",10);
        client.post()
            .uri(BASE_URL+"/invoked/"+DEVICE_ID+"/function/AuditCommandFunction")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(properties)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getProperties() {
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        add(readPropertyMessageReply);
        List<String> list = new ArrayList<>();
        list.add("temperature");
        client.post()
            .uri(BASE_URL+"/"+DEVICE_ID+"/properties")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

}