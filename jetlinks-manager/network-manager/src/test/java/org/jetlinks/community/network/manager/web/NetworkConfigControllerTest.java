package org.jetlinks.community.network.manager.web;

import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(NetworkConfigController.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NetworkConfigControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/network/config";
    private static final String ID = "test";
    @Autowired
    private NetworkConfigService configService;

    @Test
    void getService() {
        NetworkConfigService service = new NetworkConfigController(configService, Mockito.mock(NetworkManager.class)).getService();
        assertNotNull(service);
    }

    @Test
    @Order(0)
    void add(){
        assertNotNull(configService);
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
        assertNotNull(client);
        assertNotNull(configService);
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

    @Autowired
    private NetworkManager networkManager;
    @Test
    void getNetworkInfoError() {
        assertNotNull(networkManager);
        networkManager = Mockito.mock(NetworkManager.class);
        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.error(new IllegalArgumentException()));

        client.get()
            .uri(BASE_URL+"/TCP_SERVER/_detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getSupports() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/supports")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void start() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+ID+"/_start")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.post()
            .uri(BASE_URL+"/aaa/_start")
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }

    @Test
    void shutdown() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+ID+"/_shutdown")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.post()
            .uri(BASE_URL+"/aaa/_shutdown")
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }
}