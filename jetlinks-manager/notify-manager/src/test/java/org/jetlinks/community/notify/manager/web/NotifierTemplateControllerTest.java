package org.jetlinks.community.notify.manager.web;

import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.manager.service.NotifyTemplateService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;


@WebFluxTest(NotifierTemplateController.class)
class NotifierTemplateControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/notifier/template";

    @Test
    void getService() {
        ReactiveCrudService<NotifyTemplateEntity, String> service = new NotifierTemplateController(new NotifyTemplateService(), new ArrayList<>()).getService();
        assertNotNull(service);
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