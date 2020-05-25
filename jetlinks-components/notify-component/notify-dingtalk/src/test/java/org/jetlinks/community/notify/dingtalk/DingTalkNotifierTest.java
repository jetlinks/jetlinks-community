package org.jetlinks.community.notify.dingtalk;

import org.jetlinks.core.Values;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DingTalkNotifierTest {


    @Test
    @Disabled
    void test(){
        DingTalkProperties properties=new DingTalkProperties();
        properties.setAppKey("appkey");
        properties.setAppSecret("appSecuret");

        DingTalkMessageTemplate messageTemplate=new DingTalkMessageTemplate();

        messageTemplate.setAgentId("335474263");
        messageTemplate.setMessage("test"+System.currentTimeMillis());
        messageTemplate.setUserIdList("0458215455697857");

        DingTalkNotifier notifier=new DingTalkNotifier("test",
                WebClient.builder().build(),properties,null
        );

        notifier.send(messageTemplate, Values.of(new HashMap<>()))
                .as(StepVerifier::create)
                .expectComplete()
                .verify();


    }

}