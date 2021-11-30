package org.jetlinks.community.logging.controller;

import org.jetlinks.community.logging.test.spring.TestJetLinksApplication;
import org.jetlinks.community.logging.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

@WebFluxTest(AccessLoggerController.class)
class AccessLoggerControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/logger/access";
    @Test
    void getAccessLogger() {
        client.get()
            .uri(BASE_URL+"/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}