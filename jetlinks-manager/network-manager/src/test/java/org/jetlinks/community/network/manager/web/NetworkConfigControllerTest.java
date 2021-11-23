package org.jetlinks.community.network.manager.web;

import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.test.spring.TestJetLinksController;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(NetworkConfigController.class)
class NetworkConfigControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/network/config";
    private static final String ID = "test";
    @Autowired
    private NetworkConfigService configService;
    @Test
    void add(){
        NetworkConfigEntity entity = new NetworkConfigEntity();
        entity.setId(ID);
        entity.setType("TCP_SERVER");
        entity.setState(NetworkConfigState.enabled);
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("parserType", PayloadParserType.DIRECT);
        entity.setConfiguration(configuration);
        configService.save(entity).subscribe();
    }

    @Test
    void getNetworkInfo() {
        add();
        client.get()
            .uri(BASE_URL+"/TCP_SERVER/_detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        NetworkConfigEntity entity = new NetworkConfigEntity();
        entity.setId(ID);
        entity.setType("TCP_SERVER");
        entity.setState(NetworkConfigState.disabled);
        Map<String, Object> configuration = new HashMap<>();

        entity.setConfiguration(configuration);
        configService.save(entity).subscribe();
        client.get()
            .uri(BASE_URL+"/TCP_SERVER/_detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getSupports() {
        client.get()
            .uri(BASE_URL+"/supports")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void start() {
        add();
        client.post()
            .uri(BASE_URL+"/"+ID+"/_start")
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
}