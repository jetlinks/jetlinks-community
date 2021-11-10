package org.jetlinks.community.notify.manager.web;

import com.alibaba.fastjson.JSON;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.manager.service.NotifyConfigService;
import org.jetlinks.community.notify.manager.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(NotifierController.class)
class NotifierControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/notifier";

    @Autowired
    private NotifyConfigService configService;

    @Test
    void sendNotify() {
        NotifierController.SendNotifyRequest sendNotifyRequest = new NotifierController.SendNotifyRequest();
        NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
        notifyTemplateEntity.setId("test");
        notifyTemplateEntity.setTemplate("weixin");
        notifyTemplateEntity.setProvider("corpMessage");
        notifyTemplateEntity.setType("weixin");
        sendNotifyRequest.setTemplate(notifyTemplateEntity);
        sendNotifyRequest.getContext().put("test","test");

        NotifyConfigEntity notifyConfigEntity = new NotifyConfigEntity();
        notifyConfigEntity.setId("nid");
        notifyConfigEntity.setProvider("corpMessage");
        notifyConfigEntity.setName("test");
        notifyConfigEntity.setType("weixin");
        configService.save(notifyConfigEntity).subscribe();

//        String s = JSON.toJSONString(sendNotifyRequest);
        String s = "{\n" +
            "  \"template\": {\n" +
            "    \"id\": \"test\",\n" +
            "    \"type\": \"email\",\n" +
            "    \"provider\": \"corpMessage\",\n" +
            "    \"name\": \"test\",\n" +
            "    \"template\": \"test\"\n" +
            "  },\n" +
            "  \"context\": {}\n" +
            "}";
        client.post()
            .uri(BASE_URL+"/nid/_send")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}