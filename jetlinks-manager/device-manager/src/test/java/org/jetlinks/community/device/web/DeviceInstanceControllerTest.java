package org.jetlinks.community.device.web;


import com.google.common.cache.Cache;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.ImportDeviceInstanceResult;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.manager.DeviceBindProvider;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DisconnectDeviceMessageReply;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.jetlinks.supports.cluster.EventBusDeviceOperationBroker;
import org.jetlinks.supports.config.EventBusStorageManager;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.*;
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
    public static final String DEVICE_ID = "1000";
    public static final String PRODUCT_ID = "1236859833832701954";

    public static final String BASE_URL2 = "/device/product";

    public static final String BASE_URL11 = "/protocol";
    public static final String ID_1 = "demo-v1";

    @Autowired
    private LocalDeviceInstanceService service;

    @Autowired
    private DeviceRegistry registry;

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
        assertEquals(1, responseBody.size());
        assertEquals(2, responseBody.get(0).getTotal());
    }


    @Test
    @Order(3)
    void getDeviceDetailInfo() {
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.id").isEqualTo(DEVICE_ID)
            .jsonPath("$.state.text").isEqualTo("离线");
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
    @Order(3)
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
    @Order(3)
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
    @Order(3)
    void getDeviceState() {
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/state")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.text").isEqualTo("离线")
            .jsonPath("$.value").isEqualTo("offline");
    }

    @Test
    @Order(4)
    void resetConfiguration() {
        client.put()
            .uri(BASE_URL + "/" + DEVICE_ID + "/configuration/_reset")
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }


    @Autowired
    private ClusterDeviceRegistry clusterDeviceRegistry;

    @Test
    @Order(4)
    void disconnect() {
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
        deviceInstanceEntity.setId("test");
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
            .thenReturn(Flux.just(new DisconnectDeviceMessageReply()));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();


        cache.put("test", Mono.just(deviceOperator));

        Boolean responseBody = client.post()
            .uri(BASE_URL + "/test/disconnect")
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
        client.get()
            .uri(BASE_URL + "/" + DEVICE_ID + "/event/fire_alarm")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void queryDeviceLog() {
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
        assertEquals("1000", responseBody.get(0).getDeviceId());
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
        assertEquals("1000", responseBody.get(0).getDeviceId());
        assertEquals("string", responseBody.get(0).getType());
    }

    @Test
    @Order(7)
    void deleteDeviceTag() {
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
        assertEquals("test", responseBody);
    }

    @Test
    @Order(8)
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
        assertEquals("test", responseBody);

    }


    @Test
    @Order(7)
    void writeProperties() {

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
        deviceInstanceEntity.setId("test1");
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


        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        WritePropertyMessageReply writePropertyMessageReply = WritePropertyMessageReply.create();
        writePropertyMessageReply.setProperties(hashMap);

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(writePropertyMessageReply));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();


        cache.put("test1", Mono.just(deviceOperator));


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

    @Test
    @Order(7)
    void invokedFunction() {
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
        deviceInstanceEntity.setId("test2");
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

        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(functionInvokeMessageReply));


        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(), "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();


        cache.put("test2", Mono.just(deviceOperator));
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
        deviceInstanceEntity.setId("test3");
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");
        service.save(deviceInstanceEntity).subscribe();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setTransportProtocol("TCP");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map1);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(functionInvokeMessageReply));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(), "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();

        cache.put("test3", Mono.just(deviceOperator));


        Map<String, Object> map = new HashMap<>();
        map.put("event","event");

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
        deviceInstanceEntity.setId("test5");
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");
        service.save(deviceInstanceEntity).subscribe();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setTransportProtocol("TCP");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map1);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.error(new DeviceOperationException(ErrorCode.REQUEST_HANDLING)));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(), "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();

        cache.put("test5", Mono.just(deviceOperator));


        Map<String, Object> map = new HashMap<>();
        map.put("functionId","AuditCommandFunction");
        map.put("inputs",10);

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

        Map<String, Object> map = new HashMap<>();
        map.put("event","event");
        List<Map> list = new ArrayList<>();
        list.add(map);
        client.post()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/messages")
                .queryParam("where","id=test3")
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
        deviceInstanceEntity.setId("test4");
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setCreatorName("超级管理员");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setDeriveMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");
        service.save(deviceInstanceEntity).subscribe();
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setTransportProtocol("TCP");
        deviceProductEntity.setProtocolName("演示协议v1");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setCreatorId("1199596756811550720");
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setName("TCP测试");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("tcp_auth_key", "admin");
        deviceProductEntity.setConfiguration(map1);
        deviceProductEntity.setMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}");

        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setOutput("test");

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.error(new IllegalArgumentException()));

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry(new MockProtocolSupport(), standaloneDeviceMessageBroker);
        inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).subscribe();
        DeviceOperator deviceOperator = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).block();
        deviceOperator.setConfig(connectionServerId.getKey(), "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        deviceOperator.setConfig(DeviceConfigKey.productId.getKey(), "test").subscribe();
        deviceOperator.setConfig("lst_metadata_time", 1L).subscribe();

        deviceOperator.updateMetadata(
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\"," +
                "\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\"}}]," +
                "\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}]," +
                "\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe();

        cache.put("test4", Mono.just(deviceOperator));


        Map<String, Object> map = new HashMap<>();
        map.put("functionId","AuditCommandFunction");
        map.put("inputs",10);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("event","event");
        List<Map> list = new ArrayList<>();
        list.add(map);
        list.add(map2);
        client.post()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/messages")
                    .queryParam("where","id=test4")
                    .build()
            )
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is5xxServerError();

//        client.post()
//            .uri(uriBuilder ->
//                uriBuilder.path(BASE_URL + "/messages")
//                    .queryParam("where","id=test4")
//                    .build()
//            )
//            .contentType(MediaType.APPLICATION_JSON)
//            .bodyValue(list)
//            .exchange()
//            .expectStatus()
//            .is2xxSuccessful();
    }

    @Test
    @Order(5)
    void updateMetadata() {
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
        client.post()
            .uri(BASE_URL + "/" + DEVICE_ID + "/undeploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }


    @Test
    @Order(11)
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
        assertEquals(1, responseBody);
    }

    @Test
    @Order(12)
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
        assertEquals(1, responseBody);
    }

    //删除放在最后
    @Test
    @Order(13)
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
        assertEquals(1, responseBody);
    }
}