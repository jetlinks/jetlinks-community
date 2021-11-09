package org.jetlinks.community.device.web;


import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.ImportDeviceInstanceResult;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.manager.DeviceBindProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.supports.cluster.EventBusDeviceOperationBroker;
import org.jetlinks.supports.config.EventBusStorageManager;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

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
    public static final String DEVICE_ID = "1000";
    public static final String PRODUCT_ID = "1236859833832701954";

    public static final String BASE_URL2 = "/device/product";

    public static final String BASE_URL11 = "/protocol";
    public static final String ID_1 = "demo-v1";

    @Autowired
    private LocalDeviceInstanceService service;

    //先添加协议
    void addProtocol() {
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte) 1);
        protocolSupportEntity.setType("jar");
        Map<String, Object> map = new HashMap<>();

        map.put("provider", "org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        map.put("location", "http://localhost:8848/upload/20211008/1446352693262381056.jar");
        protocolSupportEntity.setConfiguration(map);
        client.patch()
            .uri(BASE_URL11)
            .bodyValue(protocolSupportEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
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
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}");

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
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DEVICE_ID);
        //deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}"
        );
        //String s = "{\"creatorIdProperty\":\"creatorId\",\"creatorName\":\"超级管理员\",\"deriveMetadata\":\"{\\\"events\\\":[{\\\"id\\\":\\\"fire_alarm\\\",\\\"name\\\":\\\"火警报警\\\",\\\"expands\\\":{\\\"level\\\":\\\"urgent\\\"},\\\"valueType\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":[{\\\"id\\\":\\\"lat\\\",\\\"name\\\":\\\"纬度\\\",\\\"valueType\\\":{\\\"type\\\":\\\"float\\\"}},{\\\"id\\\":\\\"point\\\",\\\"name\\\":\\\"点位\\\",\\\"valueType\\\":{\\\"type\\\":\\\"int\\\"}},{\\\"id\\\":\\\"lnt\\\",\\\"name\\\":\\\"经度\\\",\\\"valueType\\\":{\\\"type\\\":\\\"float\\\"}}]}}],\\\"properties\\\":[{\\\"id\\\":\\\"temperature\\\",\\\"name\\\":\\\"温度\\\",\\\"valueType\\\":{\\\"type\\\":\\\"float\\\",\\\"scale\\\":2,\\\"unit\\\":\\\"celsiusDegrees\\\"},\\\"expands\\\":{\\\"readOnly\\\":\\\"true\\\",\\\"source\\\":\\\"device\\\"}}],\\\"functions\\\":[],\\\"tags\\\":[{\\\"id\\\":\\\"test\\\",\\\"name\\\":\\\"tag\\\",\\\"valueType\\\":{\\\"type\\\":\\\"int\\\",\\\"unit\\\":\\\"meter\\\"},\\\"expands\\\":{\\\"readOnly\\\":\\\"false\\\"}}]}\",\"id\":\"1000\",\"name\":\"TCP-setvice\",\"productId\":\"1236859833832701954\",\"productName\":\"TCP测试\"}";
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
    void getDeviceDetailInfo() {
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.id").isEqualTo(DEVICE_ID)
            .jsonPath("$.state.text").isEqualTo("未激活");
    }

    @Test
    @Order(3)
    void getDeviceConfigMetadata() {
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/config-metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("TCP认证配置");
    }

    @Test
    @Order(4)
    void getExpandsConfigMetadata() {
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
    @Order(5)
    void getBindProviders() {
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
    @Order(6)
    void getDeviceState() {
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/state")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.text").isEqualTo("未激活")
            .jsonPath("$.value").isEqualTo("notActive");
    }

    @Test
    @Order(7)
    void deviceDeploy() {
        DeviceDeployResult responseBody = client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(DeviceDeployResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getTotal());

    }

    @Test
    @Order(8)
    void resetConfiguration() {
        client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/configuration/_reset")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }

    @Test
    @Order(9)
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
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(0).getTotal());
    }

    //放在最后
    @Test
    @Order(10)
    void unDeploy() {
        DeviceDeployResult responseBody = client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/undeploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(DeviceDeployResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getTotal());

    }

    @Autowired
    private DeviceRegistry registry;
    @Test
    void disconnect() {
        deviceDeploy();
        syncDeviceState();
        eventBusStorageManager.getStorage("device:" + DEVICE_ID)
            .switchIfEmpty(Mono.error(new NullPointerException()))
            .map(s->s.setConfig(connectionServerId.getKey(),"test"))
            .subscribe(System.out::println);
//        eventBusDeviceOperationBroker.handleSendToDeviceMessage();
        //InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        registry.getDevice(DEVICE_ID).flatMap(s->s.online("test","test")).subscribe(System.out::println);
        Boolean responseBody = client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/disconnect")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
        assertNotNull(responseBody);
//        assertNull(responseBody);
        assertEquals(true, responseBody);

    }


    @Test
    @Order(11)
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
    @Order(12)
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
        assertEquals(0,responseBody.size());
    }

    @Test
    @Order(13)
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
        assertEquals(0,responseBody.size());

    }

    @Test
    @Order(14)
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
    @Order(15)
    void queryDeviceProperties() {
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/properties/_query")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }

    @Test
    @Order(16)
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
    @Order(17)
    void queryPagerByDeviceEvent() {
        PagerResult<?> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/event/fire_alarm")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(PagerResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(0,responseBody.getTotal());
    }

    @Test
    @Order(18)
    void queryDeviceLog() {
        PagerResult<?> responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/logs")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(PagerResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        //System.out.println(responseBody.getData());
    }

    //删除放在最后
    @Test
    @Order(19)
    void deleteBatch() {
        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
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
        assertEquals(1,responseBody);
    }

    @Test
    @Order(20)
    void unDeployBatch() {
        List<String> list = new ArrayList<>();
        list.add(DEVICE_ID);
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
        assertEquals(1,responseBody);
    }

    @Test
    @Order(21)
    void deployBatch() {
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
        assertEquals(1,responseBody);
    }

    //标签相关测试
    @Test
    @Order(22)
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
        assertEquals("1000",responseBody.get(0).getDeviceId());
        assertEquals("string",responseBody.get(0).getType());
        assertEquals("v",responseBody.get(0).getValue());
        return responseBody;
    }

    @Test
    @Order(23)
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
        assertEquals("1000",responseBody.get(0).getDeviceId());
        assertEquals("string",responseBody.get(0).getType());
    }

    @Test
    @Order(24)
    void deleteDeviceTag() {
        List<DeviceTagEntity> deviceTags = getDeviceTags();
        client.delete()
            .uri(BASE_URL + "/" + DEVICE_ID + "/tag/"+deviceTags.get(0).getId())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    //导入导出数据
    @Test
    @Order(25)
    void doBatchImportByProduct() {
        String fileUrl=this.getClass().getClassLoader().getResource("6F04AE20.xlsx").getPath();
        System.out.println(fileUrl);
        List<ImportDeviceInstanceResult> responseBody = client.get()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/" + PRODUCT_ID + "/import")
                    .queryParam("fileUrl", fileUrl)
                    .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(ImportDeviceInstanceResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1,responseBody.get(0).getResult().getTotal());
    }

    @Test
    @Order(26)
    void downloadExportTemplate() {
        String format = "xlsx";
        client.get()
            .uri(BASE_URL+"/"+PRODUCT_ID+"/template."+format)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(27)
    void export() {
        String format = "xlsx";
        client.get()
            .uri(BASE_URL+"/"+PRODUCT_ID+"/export."+format)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(28)
    void testExport() {
        String format = "xlsx";
        client.get()
            .uri(BASE_URL+"/export."+format)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(29)
    void setDeviceShadow() {
        String shadow = "test";
        String responseBody = client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/shadow")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(shadow)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("test",responseBody);
    }

    @Test
    @Order(30)
    void getDeviceShadow() {
        String responseBody = client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/shadow")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("test",responseBody);

    }

    @Autowired
    private EventBusStorageManager eventBusStorageManager;
    @Test
    @Order(31)
    void writeProperties() {
        syncDeviceState();
        eventBusStorageManager.getStorage("device:" + DEVICE_ID)
            .switchIfEmpty(Mono.error(new NullPointerException()))
            .map(s->s.setConfig(connectionServerId.getKey(),"test"))
            .subscribe(System.out::println);
//        ConfigStorage block = eventBusStorageManager.getStorage("device:" + DEVICE_ID).block();
//        eventBusStorageManager.getStorage("device:" + DEVICE_ID)
//            .map(s->s.setConfig(connectionServerId.getKey(),"test"))
//            .subscribe(System.out::println);
//        block.getConfig(connectionServerId.getKey()).subscribe(System.out::println);
        Map<String,Object> map = new HashMap<>();
        map.put("temperature",36.5);
        Map<?,?> responseBody = client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/property")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Map.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
    }

    @Test
    @Order(32)
    void invokedFunction() {
        syncDeviceState();
        String functionId = "f";
        Map<String,Object> map = new HashMap<>();
        //map.put("temperature",36.5);
        Map<?,?> responseBody = client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/function/"+functionId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Map.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
    }

    @Test
    @Order(33)
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
        Map<?,?> responseBody = client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/agg/_query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Map.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);

    }

    @Test
    @Order(34)
    void sendMessage() {
        Map<String,Object> map = new HashMap<>();
        //map.put("temperature",36.5);
        Map<?,?> responseBody = client.post()
            .uri(BASE_URL +"/"+DEVICE_ID+ "/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Map.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
    }

    @Test
    @Order(35)
    void testSendMessage() {
        Map<String,Object> map = new HashMap<>();
        //map.put("temperature",36.5);
        Map<?,?> responseBody = client.post()
            .uri(BASE_URL + "/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Map.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
    }

    @Test
    @Order(36)
    void updateMetadata() {
        String metadata = "test";
        client.put()
            .uri(BASE_URL+"/"+DEVICE_ID+"/metadata")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(metadata)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(37)
    void resetMetadata() {
        client.delete()
            .uri(BASE_URL+"/"+DEVICE_ID+"/metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}