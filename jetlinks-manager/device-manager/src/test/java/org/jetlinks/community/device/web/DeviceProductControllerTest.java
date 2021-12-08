package org.jetlinks.community.device.web;


import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


@WebFluxTest({DeviceProductController.class, ProtocolSupportController.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeviceProductControllerTest extends TestJetLinksController {

    public static final String BASE_URL = "/device/product";
    public static final String PRODUCT_ID = "1236859833832701954";

    public static final String BASE_URL11 = "/protocol";
    public static final String ID_1 = "demo-v1";

    @Autowired
    private DeviceRegistry deviceRegistry;


    void add1() {
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte) 1);
        protocolSupportEntity.setType("jar");
        Map<String, Object> map = new HashMap<>();
        //{"provider":"org.jetlinks.demo.protocol.DemoProtocolSupportProvider","location":"http://localhost:8848/upload/20211008/1446352693262381056.jar"}
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

    void deploy() {
        add1();
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

    @Test
    @Order(0)
    void add() {
        deploy();
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
            .uri(BASE_URL)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deviceProductEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void getDeviceConfigMetadata() {
        client.get()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/config-metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("TCP认证配置")
            .jsonPath("$[0].properties[0].property").isEqualTo("tcp_auth_key");

    }

    @Test
    @Order(1)
    void getExpandsConfigMetadata() {
        List<ConfigMetadata> responseBody = client.get()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/config-metadata/property/temperature/float")
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
        client.post()
            .uri("/device/product/metadata/convert-to/{id}", "jetlinks")
            .bodyValue("{\"properties\":[]}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(1)
    void convertMetadataFrom() {
        client.post()
            .uri("/device/product/metadata/convert-from/{id}", "jetlinks")
            .bodyValue("{\"properties\":[]}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(1)
    void deviceDeploy() {
        Integer responseBody = client.post()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/deploy")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertEquals(1, responseBody);
    }

    @Test
    @Order(3)
    void cancelDeploy() {
        Integer responseBody = client.post()
            .uri(BASE_URL + "/" + PRODUCT_ID + "/undeploy")
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
        deviceDeploy();
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
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$[0].property").isEqualTo(0);
    }
}