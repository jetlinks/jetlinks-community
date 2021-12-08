package org.jetlinks.community.device.web;


import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.community.device.web.protocol.ProtocolInfo;
import org.jetlinks.community.device.web.protocol.TransportInfo;
import org.jetlinks.community.device.web.request.ProtocolDecodePayload;
import org.jetlinks.community.device.web.request.ProtocolDecodeRequest;
import org.jetlinks.community.device.web.request.ProtocolEncodePayload;
import org.jetlinks.community.device.web.request.ProtocolEncodeRequest;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(ProtocolSupportController.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProtocolSupportControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/protocol";
    public static final String ID_1 = "demo-v1";


    void add(){
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte)1);
        protocolSupportEntity.setType("jar");
        Map<String, Object> map = new HashMap<>();
        //{"provider":"org.jetlinks.demo.protocol.DemoProtocolSupportProvider","location":"http://localhost:8848/upload/20211008/1446352693262381056.jar"}
        map.put("provider","org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        map.put("location","http://localhost:8848/upload/20211008/1446352693262381056.jar");
        protocolSupportEntity.setConfiguration(map);
        client.patch()
            .uri(BASE_URL)
            .bodyValue(protocolSupportEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(0)
    void deploy() {
        add();
        Boolean responseBody = client.post()
            .uri(BASE_URL + "/" + ID_1 + "/_deploy")
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
    @Order(3)
    void unDeploy() {
        deploy();
        Boolean responseBody = client.post()
            .uri(BASE_URL + "/" + ID_1 + "/_un-deploy")
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
    @Order(1)
    void getProviders() {
        List<String> responseBody = client.get()
            .uri(BASE_URL + "/providers")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(String.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1,responseBody.size());
        assertEquals("jar",responseBody.get(0));
    }

    @Test
    @Order(1)
    void allProtocols() {
        List<ProtocolInfo> responseBody=client.get()
            .uri(BASE_URL + "/supports")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(ProtocolInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(2,responseBody.size());

    }

    @Test
    @Order(2)
    void getTransportConfiguration() {
        client.get()
            .uri(BASE_URL + "/" + ID_1 + "/TCP/configuration")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.name").isEqualTo("TCP认证配置")
            .jsonPath("$.properties[0].property").isEqualTo("tcp_auth_key");
    }

    @Test
    @Order(1)
    void getDefaultMetadata() {
        String responseBody = client.get()
            .uri(BASE_URL + "/" + ID_1 + "/TCP/metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertEquals("{}",responseBody);

    }

    @Test
    @Order(1)
    void getAllTransport() {
        List<TransportInfo> responseBody = client.get()
            .uri(BASE_URL + "/" + ID_1 + "/transports")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(TransportInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(6,responseBody.size());
        assertEquals("TCP",responseBody.get(0).getName());
        assertEquals("UDP",responseBody.get(1).getName());
        assertEquals("MQTT",responseBody.get(2).getName());
    }

    @Test
    @Order(1)
    void convertToDetail() {
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte)1);
        protocolSupportEntity.setType("jar");
        Map<String, Object> map = new HashMap<>();
        map.put("provider","org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        map.put("location","http://localhost:8848/upload/20211008/1446352693262381056.jar");
        protocolSupportEntity.setConfiguration(map);
        client.post()
            .uri(BASE_URL + "/convert")
            .bodyValue(protocolSupportEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.id").isEqualTo("demo-v1")
            .jsonPath("$.name").isEqualTo("演示协议v1")
            .jsonPath("$.transports[0].id").isEqualTo("TCP")
            .jsonPath("$.transports[0].name").isEqualTo("TCP")
            .jsonPath("$.transports[1].id").isEqualTo("UDP")
            .jsonPath("$.transports[1].name").isEqualTo("UDP");


    }

    @Test
    @Order(1)
    void decode() {
        convertToDetail();
        ProtocolDecodeRequest protocolDecodeRequest = new ProtocolDecodeRequest();
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte)1);
        protocolSupportEntity.setType("jar");
        Map<String, Object> map = new HashMap<>();
        map.put("provider","org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        map.put("location","http://localhost:8848/upload/20211008/1446352693262381056.jar");
        protocolSupportEntity.setConfiguration(map);

        ProtocolDecodePayload protocolDecodePayload = new ProtocolDecodePayload();
        protocolDecodePayload.setPayload("1234");
        protocolDecodePayload.setPayloadType(PayloadType.STRING);
        protocolDecodePayload.setTransport(DefaultTransport.MQTT);

        protocolDecodeRequest.setEntity(protocolSupportEntity);
        protocolDecodeRequest.setRequest(protocolDecodePayload);

        String responseBody = client.post()
            .uri(BASE_URL + "/decode")
            .bodyValue(protocolDecodeRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertEquals ("[]",responseBody);


        map.put("location","http://localhost:8848/upload/20211008/144635269326238105.jar");
        client.post()
            .uri(BASE_URL + "/decode")
            .bodyValue(protocolDecodeRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(1)
    void encode() {

        ProtocolEncodeRequest protocolEncodeRequest = new ProtocolEncodeRequest();
        ProtocolSupportEntity protocolSupportEntity = new ProtocolSupportEntity();
        protocolSupportEntity.setId(ID_1);
        protocolSupportEntity.setName("演示协议v1");
        protocolSupportEntity.setState((byte)1);
        protocolSupportEntity.setType("jar");
        Map<String, Object> map = new HashMap<>();
        map.put("provider","org.jetlinks.demo.protocol.DemoProtocolSupportProvider");
        map.put("location","http://localhost:8848/upload/20211008/1446352693262381056.jar");
        protocolSupportEntity.setConfiguration(map);

        ProtocolEncodePayload protocolEncodePayload = new ProtocolEncodePayload();
        protocolEncodePayload.setPayload("1234");
        protocolEncodePayload.setPayloadType(PayloadType.JSON);
        protocolEncodePayload.setTransport(DefaultTransport.TCP);

        protocolEncodeRequest.setEntity(protocolSupportEntity);
        protocolEncodeRequest.setRequest(protocolEncodePayload);

        String responseBody = client.post()
            .uri(BASE_URL + "/encode")
            .bodyValue(protocolEncodeRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
    }

    @Test
    @Order(1)
    void allUnits() {
        client.get()
            .uri(BASE_URL + "/units")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("百分比")
            .jsonPath("$[0].id").isEqualTo("percent")
            .jsonPath("$[0].text").isEqualTo("百分比(%)");
    }
}