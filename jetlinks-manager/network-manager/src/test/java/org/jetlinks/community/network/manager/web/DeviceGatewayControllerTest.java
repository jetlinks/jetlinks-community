package org.jetlinks.community.network.manager.web;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.DeviceGatewayService;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(DeviceGatewayController.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeviceGatewayControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/gateway/device";
    private static final String ID = "test";

    @Autowired
    private NetworkConfigService networkConfigService;

    @Test
    void getService() {
        DeviceGatewayService service = new DeviceGatewayController(Mockito.mock(DeviceGatewayManager.class), Mockito.mock(DeviceGatewayService.class)).getService();
        assertNotNull(service);
    }

    @Test
    @Order(0)
    void add(){
        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId(ID);
        entity.setNetworkId("test");
        entity.setName("test");
        //entity.setState(NetworkConfigState.enabled);
        entity.setProvider("tcp-server-gateway");
        Map<String, Object> map =  new HashMap<>();
        map.put("protocol", "test");
        entity.setConfiguration(map);
            //  deviceGatewayService.save(entity).subscribe(System.out::println);
        assertNotNull(client);
        assertNotNull(networkConfigService);
        client.patch()
            .uri(BASE_URL)
            .bodyValue(entity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

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
    @Order(1)
    void startup() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+ID+"/_startup")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void pause() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+ID+"/_pause")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void shutdown() {
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/"+ID+"/_shutdown")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void getMessages() {

        DeviceGatewayService gatewayService = Mockito.mock(DeviceGatewayService.class);
        DeviceGatewayManager gatewayManager = Mockito.mock(DeviceGatewayManager.class);
        DeviceGateway deviceGateway = Mockito.mock(DeviceGateway.class);
        Mockito.when(gatewayManager.getGateway(Mockito.anyString()))
            .thenReturn(Mono.just(deviceGateway));
        Mockito.when(deviceGateway.onMessage())
            .thenReturn(Flux.just(new ReadPropertyMessage()));
        DeviceGatewayController controller = new DeviceGatewayController(gatewayManager, gatewayService);
        Flux<Message> messages = controller.getMessages(ID);
        assertNotNull(messages);

    }

    @Test
    @Order(2)
    void getProviders() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/providers")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}