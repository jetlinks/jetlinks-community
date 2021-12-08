package org.jetlinks.community.auth.web;

import org.hswebframework.web.system.authorization.api.entity.DimensionEntity;
import org.jetlinks.community.test.spring.TestJetLinksController1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@WebFluxTest(OrganizationController.class)
class OrganizationControllerTest1 extends TestJetLinksController1 {
    public static final String BASE_URL = "/organization";


    @Test
    void getAllOrg() {
        List<DimensionEntity> responseBody = client.get()
            .uri(BASE_URL + "/_all")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DimensionEntity.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
    }

}