package org.jetlinks.community.device.web;


import com.google.common.cache.Cache;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.device.configuration.TestMockSupport;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.StandaloneDeviceMessageBroker;
import org.jetlinks.core.device.manager.DeviceBindProvider;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.DisconnectDeviceMessageReply;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebFluxTest({DeviceInstanceController.class, DeviceProductController.class, ProtocolSupportController.class})
class DeviceInstanceControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/device/instance";
    public static final String DEVICE_ID = "10010";
    public static final String PRODUCT_ID = "1236859833832701953";

    public static final String BASE_URL2 = "/device/product";

    public static final String BASE_URL11 = "/protocol";
    public static final String ID_1 = "test";

    @Autowired
    private LocalDeviceInstanceService service;

    @Autowired
    private ProtocolSupportLoader loader;

    @Autowired
    private ClusterDeviceRegistry clusterDeviceRegistry;

    void initInstanceEntity(DeviceInstanceEntity deviceInstanceEntity) {
        deviceInstanceEntity.setId(DEVICE_ID);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
    }

    void initProductEntity(DeviceProductEntity deviceProductEntity) {
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setTransportProtocol("test");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map = new HashMap<>();
        map.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}");
    }

    //先添加协议
    void addProtocol() {
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte) 1);
        protocolSupportEntity.setType("jar");
//        Map<String, Object> map = new HashMap<>();
//
//        map.put("provider", "org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
//        map.put("location", "http://localhost:8848/upload/20211008/1446352693262381056.jar");
//        protocolSupportEntity.setConfiguration(map);
        client.patch()
            .uri(BASE_URL11)
            .bodyValue(protocolSupportEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        assertNotNull(loader);
        Mono supportMono1 = Mono.just(new TestMockSupport());
        Mockito.when(loader.load(Mockito.any(ProtocolSupportDefinition.class)))
            .thenReturn(supportMono1);
    }

    //发布协议
    void deployProtocol() {
        addProtocol();
        Boolean responseBody = client.post()
            .uri(BASE_URL11 + "/" + ID_1 + "/_deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody);
    }


    //再添加产品
    void addProduct() {
        deployProtocol();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);
        assertNotNull(client);
        client.patch()
            .uri(BASE_URL2)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deviceProductEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    //发布产品
    void deployProduct() {
        addProduct();
        assertNotNull(client);
        Integer responseBody = client.post()
            .uri(BASE_URL2 + "/" + PRODUCT_ID + "/deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertEquals(1, responseBody);
    }

    @Test
    @Order(1)
    void add() {
        deployProduct();
        assertNotNull(client);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initInstanceEntity(deviceInstanceEntity);
        client.post()
            .uri(BASE_URL)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(deviceInstanceEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.id").isEqualTo(DEVICE_ID)
            .jsonPath("$.name").isEqualTo("TCP-setvice")
            .jsonPath("$.state.text").isEqualTo("未激活");

        deviceInstanceEntity.setState(DeviceState.online);
        service.save(deviceInstanceEntity).map(SaveResult::getTotal).subscribe();

        service.findById(DEVICE_ID).map(DeviceInstanceEntity::getState).subscribe();
    }

    @Test
    @Order(2)
    void deviceDeploy() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(4)
    void deployAll() {
        List<DeviceDeployResult> responseBody = client.get()
            .uri(BASE_URL + "/deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceDeployResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
    }


    @Test
    @Order(3)
    void getDeviceDetailInfo() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void getDeviceConfigMetadata() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/config-metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void getExpandsConfigMetadata() {
        assertNotNull(client);
        List<ConfigMetadata> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/config-metadata/property/temperature/float")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(ConfigMetadata.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(0, responseBody.size());
    }

    @Test
    @Order(3)
    void getBindProviders() {
        assertNotNull(client);
        List<DeviceBindProvider> responseBody = client.get()
            .uri(BASE_URL + "/bind-providers")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceBindProvider.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(0, responseBody.size());
    }

    @Test
    @Order(3)
    void getDeviceState() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/state")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.text").isEqualTo("离线");
    }

    @Test
    @Order(4)
    void resetConfiguration() {
        assertNotNull(client);
        client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/configuration/_reset")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }


    void reply(String id,DeviceMessageReply deviceMessageReply){

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
        initInstanceEntity(deviceInstanceEntity);
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(deviceMessageReply));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();

        cache.put(id, Mono.just(deviceOperator));
    }

    @Test
    @Order(4)
    void disconnect() {
        assertNotNull(clusterDeviceRegistry);
        DisconnectDeviceMessageReply disconnectDeviceMessageReply = new DisconnectDeviceMessageReply();
        reply("test",disconnectDeviceMessageReply);
        Boolean responseBody = client.post()
            .uri(BASE_URL + "/test/disconnect")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(true, responseBody);

    }


    @Test
    @Order(5)
    void syncDeviceState() {
        List<Integer> responseBody = client.get()
            .uri(BASE_URL + "/state/_sync")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody.get(0));
        assertEquals(1, responseBody.size());

    }

    @Test
    @Order(3)
    void getDeviceLatestProperties() {
        List<DeviceProperty> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/properties/latest")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceProperty.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(0, responseBody.size());
    }

    @Test
    @Order(3)
    void testGetDeviceLatestProperties() {
        List<DeviceProperty> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/properties")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceProperty.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(0, responseBody.size());

    }

    @Test
    @Order(3)
    void getDeviceLatestProperty() {
        DeviceProperty responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/property/temperature")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(DeviceProperty.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
        assertNull(responseBody);
    }

    @Test
    @Order(3)
    void queryDeviceProperties() {
        assertNotNull(client);
        client.get()
            .uri(uri -> uri.path(BASE_URL + "/" + DEVICE_ID + "/properties/_query")
                .queryParam("terms[0].column", "property")
                .queryParam("terms[0].value", "test")
                .build()
            )
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    // ---------
    @Test
    @Order(3)
    void testQueryDeviceProperties() {
        PagerResult<?> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/property/temperature/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(PagerResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody.getTotal());
    }

    @Test
    @Order(3)
    void queryPagerByDeviceEvent() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/event/fire_alarm")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void queryDeviceLog() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/logs")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }


    //标签相关测试
    @Test
    @Order(6)
    List<DeviceTagEntity> getDeviceTags() {
        List<DeviceTagEntity> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/tags")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceTagEntity.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("10010", responseBody.get(0).getDeviceId());
        assertEquals("string", responseBody.get(0).getType());
        assertEquals("v", responseBody.get(0).getValue());
        return responseBody;
    }

    @Test
    @Order(5)
    void saveDeviceTag() {
        DeviceTagEntity deviceTagEntity = new DeviceTagEntity();
        deviceTagEntity.setDeviceId(DEVICE_ID);
        deviceTagEntity.setName("test");
        deviceTagEntity.setKey("k");
        deviceTagEntity.setName("n");
        deviceTagEntity.setValue("v");
        List<DeviceTagEntity> responseBody = client.patch()
            .uri(BASE_URL + "/" + DEVICE_ID + "/tag")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deviceTagEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceTagEntity.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("10010", responseBody.get(0).getDeviceId());
        assertEquals("string", responseBody.get(0).getType());
    }

    @Test
    @Order(7)
    void deleteDeviceTag() {
        assertNotNull(client);
        List<DeviceTagEntity> deviceTags = getDeviceTags();
        client.delete()
            .uri(BASE_URL + "/" + DEVICE_ID + "/tag/" + deviceTags.get(0).getId())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    //导入导出数据
    @Test
    @Order(2)
    void doBatchImportByProduct() {
        String fileUrl = this.getClass().getClassLoader().getResource("6F04AE20.xlsx").getPath();
        System.out.println(fileUrl);
        assertNotNull(client);
        client.get()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/" + PRODUCT_ID + "/import")
                    .queryParam("fileUrl", fileUrl)
                    .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void downloadExportTemplate() {
        String format = "xlsx";
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/template." + format)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void export() {
        String format = "xlsx";
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/export." + format)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void testExport() {
        String format = "xlsx";
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/export." + format)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(7)
    void setDeviceShadow() {
        String shadow = "test";
        assertNotNull(client);
        client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/shadow")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(shadow)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(8)
    void getDeviceShadow() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/shadow")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }


    @Test
    @Order(7)
    void writeProperties() {
        assertNotNull(clusterDeviceRegistry);
        assertNotNull(client);
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        WritePropertyMessageReply writePropertyMessageReply = WritePropertyMessageReply.create();
        writePropertyMessageReply.setProperties(hashMap);
        reply("test1",writePropertyMessageReply);


        Map<String, Object> map1 = new HashMap<>();
        map1.put("temperature", 36.5);
        client.put()
            .uri(BASE_URL + "/test1/property")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map1)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    void replySet(String id,Flux<DeviceMessageReply> deviceMessageReply,StandaloneDeviceMessageBroker standaloneDeviceMessageBroker){
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
        initInstanceEntity(deviceInstanceEntity);
        service.save(deviceInstanceEntity).subscribe();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);



        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(deviceMessageReply);


        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(deviceOperator);
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(), "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();


        cache.put(id, Mono.just(deviceOperator));
    }

    @Test
    @Order(7)
    void invokedFunction() {
        assertNotNull(clusterDeviceRegistry);
        assertNotNull(client);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");
        replySet("test2",Flux.just(functionInvokeMessageReply),standaloneDeviceMessageBroker);

        String functionId = "AuditCommandFunction";
        Map<String, Object> map1 = new HashMap<>();
        //map.put("temperature",36.5);
        client.post()
            .uri(BASE_URL + "/test2/function/" + functionId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map1)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(7)
    void aggDeviceProperty() {
        String s = "{\n" +
            "  \"columns\": [\n" +
            "    {\n" +
            "      \"property\": \"property\",\n" +
            "      \"alias\": \"property\",\n" +
            "      \"agg\": \"COUNT\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"query\": {\n" +
            "    \"interval\": \"1d\",\n" +
            "    \"format\": \"\",\n" +
            "    \"from\": \"\",\n" +
            "    \"to\": \"\",\n" +
            "    \"limit\": 0,\n" +
            "    \"filter\": {\n" +
            "      \"terms\": [\n" +
            "        \n" +
            "      ],\n" +
            "      \"includes\": [],\n" +
            "      \"excludes\": [],\n" +
            "      \"paging\": true,\n" +
            "      \"firstPageIndex\": 0,\n" +
            "      \"pageIndex\": 0,\n" +
            "      \"pageSize\": 0,\n" +
            "      \"sorts\": [\n" +
            "        {\n" +
            "          \"name\": \"\",\n" +
            "          \"order\": \"\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"context\": {},\n" +
            "      \"where\": \"\",\n" +
            "      \"orderBy\": \"\",\n" +
            "      \"total\": 0,\n" +
            "      \"parallelPager\": true\n" +
            "    }\n" +
            "  }\n" +
            "}";
        assertNotNull(client);
        client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/agg/_query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(8)
    void sendMessage() {

        assertNotNull(clusterDeviceRegistry);
        assertNotNull(client);
        assertNotNull(service);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");
        replySet("test3",Flux.just(functionInvokeMessageReply),standaloneDeviceMessageBroker);

        Map<String, Object> map = new HashMap<>();
        map.put("event", "event");

        client.post()
            .uri(BASE_URL + "/test3/message")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(8)
    void sendMessage1() {

        assertNotNull(clusterDeviceRegistry);
        assertNotNull(client);
        assertNotNull(service);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");
        replySet("test5",Flux.just(functionInvokeMessageReply),standaloneDeviceMessageBroker);

        Map<String, Object> map = new HashMap<>();
        map.put("functionId", "AuditCommandFunction");
        map.put("inputs", 10);

        client.post()
            .uri(BASE_URL + "/test5/message")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.error(new IllegalArgumentException()));

        client.post()
            .uri(BASE_URL + "/test5/message")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }

    @Test
    @Order(9)
    void testSendMessage() {
        assertNotNull(client);
        Map<String, Object> map = new HashMap<>();
        map.put("event", "event");
        List<Map> list = new ArrayList<>();
        list.add(map);
        client.post()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/messages")
                    .queryParam("where", "id=test3")
                    .build()
            )
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(9)
    void testSendMessage1() {
        assertNotNull(client);
        assertNotNull(clusterDeviceRegistry);
        assertNotNull(service);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        replySet("test4",Flux.error(new IllegalArgumentException()),standaloneDeviceMessageBroker);

        Map<String, Object> map = new HashMap<>();
        map.put("functionId", "AuditCommandFunction");
        map.put("inputs", 10);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("event", "event");
        List<Map> list = new ArrayList<>();
        list.add(map);
        list.add(map2);
        client.post()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/messages")
                    .queryParam("where", "id=test4")
                    .build()
            )
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(5)
    void updateMetadata() {
        assertNotNull(client);
        String metadata = "test";
        client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/metadata")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(metadata)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(6)
    void resetMetadata() {
        assertNotNull(client);
        client.delete()
            .uri(BASE_URL + "/" + DEVICE_ID + "/metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }


    //注销
    @Test
    @Order(10)
    void unDeploy() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/undeploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }


    @Test
    @Order(11)
    void deployBatch() {
        assertNotNull(client);
        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
        Integer responseBody = client.put()
            .uri(BASE_URL + "/batch/_deploy")
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody);
    }

    @Test
    @Order(12)
    void unDeployBatch() {
        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
        assertNotNull(client);
        Integer responseBody = client.put()
            .uri(BASE_URL + "/batch/_unDeploy")
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody);
    }

    //删除放在最后
    @Test
    @Order(13)
    void deleteBatch() {
        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
        assertNotNull(client);
        Integer responseBody = client.put()
            .uri(BASE_URL + "/batch/_delete")
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody);
    }
}