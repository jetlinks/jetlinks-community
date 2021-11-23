package org.jetlinks.community.network.manager.web;

import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.DeviceGatewayService;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.test.spring.TestJetLinksController;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(DeviceGatewayController.class)
class DeviceGatewayControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/gateway/device";
    private static final String ID = "test";

    @Autowired
    private DeviceGatewayService deviceGatewayService;
    @Autowired
    private NetworkConfigService networkConfigService;
    void add(){

        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId(ID);
        entity.setNetworkId("test");
        entity.setName("test");
        entity.setState(NetworkConfigState.enabled);
        entity.setProvider("tcp-server-gateway");
        Map<String, Object> map =  new HashMap<>();
        map.put("protocol", "test");
        entity.setConfiguration(map);
        deviceGatewayService.save(entity).subscribe();

        NetworkConfigEntity configEntity = new NetworkConfigEntity();
        configEntity.setId("test");
        Map<String, Object> map1=  new HashMap<>();
        map1.put("parserType", PayloadParserType.DIRECT);
        configEntity.setConfiguration(map1);
        configEntity.setState(NetworkConfigState.enabled);
        configEntity.setName("test");
        configEntity.setType("TCP_SERVER");
        configEntity.setDescription("test");
        networkConfigService.save(configEntity).subscribe();
    }

    @Test
    void startup() {
        add();
        client.post()
            .uri(BASE_URL+"/"+ID+"/_startup")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void pause() {
        add();
        client.post()
            .uri(BASE_URL+"/"+ID+"/_pause")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void shutdown() {
        add();
        client.post()
            .uri(BASE_URL+"/"+ID+"/_shutdown")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getMessages() {
    }

    @Test
    void getProviders() {
    }
}