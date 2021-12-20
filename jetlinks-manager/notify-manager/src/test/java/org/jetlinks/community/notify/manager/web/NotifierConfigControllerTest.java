package org.jetlinks.community.notify.manager.web;

import org.jetlinks.community.notify.manager.service.NotifyConfigService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(NotifierConfigController.class)
class NotifierConfigControllerTest extends TestJetLinksController {

    private static final String BASE_URL = "/notifier/config";


    @Test
    void getService() {
        NotifierConfigController configController = new NotifierConfigController(Mockito.mock(NotifyConfigService.class), Mockito.mock(List.class));
        NotifyConfigService service = configController.getService();
        assertNotNull(service);
    }

    @Test
    void getAllTypes() {
        client.get()
            .uri(BASE_URL+"/sms/test/metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void testGetAllTypes() {
        client.get()
            .uri(BASE_URL+"/types")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void getTypeProviders() {
        client.get()
            .uri(BASE_URL+"/type/sms/providers")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}