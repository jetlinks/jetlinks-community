package org.jetlinks.community.notify.manager.web;


import com.alibaba.fastjson.JSON;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.manager.service.NotifyConfigService;
import org.jetlinks.community.notify.sms.PlainTextSmsTemplate;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@WebFluxTest(NotifierController.class)
class NotifierControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/notifier";

    @Autowired
    private NotifyConfigService configService;

//    @Autowired
//    private NotifierManager notifierManager;

    @Test
    void sendNotify() {
        NotifierController.SendNotifyRequest sendNotifyRequest = new NotifierController.SendNotifyRequest();
        NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
        notifyTemplateEntity.setId("test");
        notifyTemplateEntity.setProvider("test");
        notifyTemplateEntity.setType("sms");
        PlainTextSmsTemplate plainTextSmsTemplate = new PlainTextSmsTemplate();
        plainTextSmsTemplate.setText("sing");
        List<String> list = new ArrayList<>();
        list.add("a");
        plainTextSmsTemplate.setSendTo(list);
        String s = JSON.toJSONString(plainTextSmsTemplate);
        notifyTemplateEntity.setTemplate(s);


        sendNotifyRequest.setTemplate(notifyTemplateEntity);
        sendNotifyRequest.getContext().put("test","test");

        NotifyConfigEntity notifyConfigEntity = new NotifyConfigEntity();
        notifyConfigEntity.setId("nid");
        notifyConfigEntity.setProvider("test");
        notifyConfigEntity.setName("test");
        notifyConfigEntity.setType("sms");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("regionId","regionId");
        configuration.put("accessKeyId","accessKeyId");
        configuration.put("secret","secret");
        notifyConfigEntity.setConfiguration(configuration);
        configService.save(notifyConfigEntity).subscribe();


        client.post()
            .uri(BASE_URL+"/nid/_send")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(sendNotifyRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}