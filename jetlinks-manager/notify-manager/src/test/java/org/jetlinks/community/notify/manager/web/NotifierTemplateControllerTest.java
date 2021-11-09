package org.jetlinks.community.notify.manager.web;

import org.jetlinks.community.notify.manager.service.NotifyTemplateService;
import org.jetlinks.community.notify.manager.test.spring.TestJetLinksController;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(NotifierTemplateController.class)
class NotifierTemplateControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/notifier/template";

    @Test
    void getService() {
        new NotifierTemplateController(new NotifyTemplateService(),new ArrayList<>()).getService();
    }

    @Test
    void getAllTypes() {
        client.get()
            .uri(BASE_URL+"/weixin/corpMessage/config/metadata")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.name").isEqualTo("模版配置")
            .jsonPath("$.properties[0].name").isEqualTo("应用ID");
    }
}