package org.jetlinks.community.network.manager.web;

import org.jetlinks.community.network.manager.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(CertificateController.class)
class CertificateControllerTest extends TestJetLinksController {
    private static final String BASE_URL = "/network/certificate";
    private static final String ID = "test";

    @Test
    void getService() {

    }

    @Test
    void getCertificateInfo() {
        client.get()
            .uri(BASE_URL+"/"+ID+"/detail")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void upload() {
    }
}