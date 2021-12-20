package org.jetlinks.community.auth.web;


import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(AuthorizationSettingDetailController.class)
class AuthorizationSettingDetailControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/autz-setting/detail";

    @Test
    void saveSettings() {
        AuthorizationSettingDetail authorizationSettingDetail = new AuthorizationSettingDetail();
        authorizationSettingDetail.setTargetId("test");
        authorizationSettingDetail.setTargetType("test");
        Boolean responseBody = client.post()
            .uri(BASE_URL + "/_save")
            .bodyValue(authorizationSettingDetail)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(true,responseBody);
    }

    @Test
    void getSettings() {
        AuthorizationSettingDetail responseBody = client.get()
            .uri(BASE_URL + "/test/test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(AuthorizationSettingDetail.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
    }
}