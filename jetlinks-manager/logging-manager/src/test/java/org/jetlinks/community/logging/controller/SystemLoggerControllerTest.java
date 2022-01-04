package org.jetlinks.community.logging.controller;


import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(SystemLoggerController.class)
class SystemLoggerControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/logger/system";
    @Test
    void getSystemLogger() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}