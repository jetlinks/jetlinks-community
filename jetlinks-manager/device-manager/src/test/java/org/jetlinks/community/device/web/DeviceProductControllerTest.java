package org.jetlinks.community.device.web;

import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.device.service.DeviceConfigMetadataManager;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@WebFluxTest({DeviceProductController.class, ProtocolSupportController.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeviceProductControllerTest extends TestJetLinksController {

    public static final String BASE_URL = "/device/product";
    public static final String PRODUCT_ID = "1236859833832701954";

    public static final String BASE_URL11 = "/protocol";
    public static final String ID_1 = "test";

//    @Autowired
//    private DeviceRegistry deviceRegistry;


//    void add1() {
//        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
//        protocolSupportEntity.setId(ID_1);
//        protocolSupportEntity.setName("演示协议v1");
//        protocolSupportEntity.setState((byte) 1);
//        protocolSupportEntity.setType("jar");
//        Map<String, Object> map = new HashMap<>();
//        //{"provider":"org.jetlinks.demo.protocol.DemoProtocolSupportProvider","location":"http://localhost:8848/upload/20211008/1446352693262381056.jar"}
//        map.put("provider", "org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
//        map.put("location", "http://localhost:8848/upload/20211008/1446352693262381056.jar");
//        protocolSupportEntity.setConfiguration(map);
//        client.patch()
//            .uri(BASE_URL11)
//            .bodyValue(protocolSupportEntity)
//            .exchange()
//            .expectStatus()
//            .is2xxSuccessful();
//    }
//
//    void deploy() {
//        add1();
//        Boolean responseBody = client.post()
//            .uri(BASE_URL11 + "/" + ID_1 + "/_deploy")
//            .exchange()
//            .expectStatus()
//            .is2xxSuccessful()
//            .expectBody(Boolean.class)
//            .returnResult()
//            .getResponseBody();
//        assertNotNull(responseBody);
//        assertEquals(true, responseBody);
//    }

    @Autowired
    private ProtocolSupportLoader loader;

    @Test
    @Order(0)
    void add() {
        Mono supportMono = Mono.just(new MockProtocolSupport());
        Mockito.when(loader.load(Mockito.any(ProtocolSupportDefinition.class)))
            .thenReturn(supportMono);
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte) 1);
        protocolSupportEntity.setType("jar");
        client.patch()
            .uri(BASE_URL11)
            .bodyValue(protocolSupportEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.post()
            .uri(BASE_URL11 + "/" + ID_1 + "/_deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }
    @Test
    void test(){
        DeviceProductController controller = new DeviceProductController(
            Mockito.mock(LocalDeviceProductService.class),new ArrayList<>(),
            Mockito.mock(DeviceDataService.class),Mockito.mock(DeviceConfigMetadataManager.class),
            Mockito.mock(ObjectProvider.class)
        );
       controller.getService();
    }

    @Test
    @Order(1)
    void getDeviceConfigMetadata() {
        client.get()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/config-metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(1)
    void getExpandsConfigMetadata() {
        client.get()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/config-metadata/property/temperature/float")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(1)
    void getMetadataCodec() {
        client.get()
            .uri(BASE_URL + "/metadata/codecs")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void convertMetadataTo() {
        String s = "{\n" +
            "  \"id\": \"test\",\n" +
            "  \"name\": \"测试\",\n" +
            "  \"properties\": [\n" +
            "    {\n" +
            "      \"id\": \"name\",\n" +
            "      \"name\": \"名称\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"string\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"functions\": [\n" +
            "    {\n" +
            "      \"id\": \"playVoice\",\n" +
            "      \"name\": \"播放声音\",\n" +
            "      \"inputs\": [\n" +
            "        {\n" +
            "          \"id\": \"text\",\n" +
            "          \"name\": \"文字内容\",\n" +
            "          \"valueType\": {\n" +
            "            \"type\": \"string\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"output\": {\n" +
            "        \"type\": \"boolean\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"events\": [\n" +
            "    {\n" +
            "      \"id\": \"temp_sensor\",\n" +
            "      \"name\": \"温度传感器\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"double\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"fire_alarm\",\n" +
            "      \"name\": \"火警\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": [\n" +
            "          {\n" +
            "            \"id\": \"location\",\n" +
            "            \"name\": \"地点\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"string\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"id\": \"lng\",\n" +
            "            \"name\": \"经度\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"double\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"id\": \"lat\",\n" +
            "            \"name\": \"纬度\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"double\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        client.post()
            .uri(BASE_URL + "/metadata/convert-to/jetlinks")
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(1)
    void convertMetadataFrom() {
        String s = "{\n" +
            "  \"id\": \"test\",\n" +
            "  \"name\": \"测试\",\n" +
            "  \"properties\": [\n" +
            "    {\n" +
            "      \"id\": \"name\",\n" +
            "      \"name\": \"名称\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"string\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"functions\": [\n" +
            "    {\n" +
            "      \"id\": \"playVoice\",\n" +
            "      \"name\": \"播放声音\",\n" +
            "      \"inputs\": [\n" +
            "        {\n" +
            "          \"id\": \"text\",\n" +
            "          \"name\": \"文字内容\",\n" +
            "          \"valueType\": {\n" +
            "            \"type\": \"string\"\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"output\": {\n" +
            "        \"type\": \"boolean\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"events\": [\n" +
            "    {\n" +
            "      \"id\": \"temp_sensor\",\n" +
            "      \"name\": \"温度传感器\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"double\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"fire_alarm\",\n" +
            "      \"name\": \"火警\",\n" +
            "      \"valueType\": {\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": [\n" +
            "          {\n" +
            "            \"id\": \"location\",\n" +
            "            \"name\": \"地点\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"string\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"id\": \"lng\",\n" +
            "            \"name\": \"经度\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"double\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"id\": \"lat\",\n" +
            "            \"name\": \"纬度\",\n" +
            "            \"valueType\": {\n" +
            "              \"type\": \"double\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        client.post()
            .uri(BASE_URL + "/metadata/convert-from/jetlinks")
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void deviceDeploy() {
        client.post()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(3)
    void cancelDeploy() {
        client.post()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/undeploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(1)
    void storePolicy() {
        List<DeviceProductController.DeviceDataStorePolicyInfo> responseBody = client.get()
            .uri(BASE_URL + "/storage/policies")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceProductController.DeviceDataStorePolicyInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(3, responseBody.size());
    }

    @Test
    @Order(1)
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
            .uri(BASE_URL + "/" + PRODUCT_ID + "/agg/_query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}