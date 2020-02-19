package org.jetlinks.community.notify.dingtalk;

import org.jetlinks.core.Values;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DingTalkNotifierTest {


    @Test
    void test(){
        DingTalkProperties properties=new DingTalkProperties();
        properties.setAppKey("dingd2rgqrqnbvgbvi9xZQ");
        properties.setAppSecret("rfSNLse4SI1CeAo6aV8cPRzig8HZACwRR6XGS_feOje-3M7rE68WjL9LKWTgko2R");

        DingTalkMessageTemplate messageTemplate=new DingTalkMessageTemplate();

        messageTemplate.setAgentId("335474263");
        messageTemplate.setMessage("test"+System.currentTimeMillis());
        messageTemplate.setUserIdList("0458215455697857");

        DingTalkNotifier notifier=new DingTalkNotifier(
                WebClient.builder().build(),properties,null
        );

        notifier.send(messageTemplate, Values.of(new HashMap<>()))
                .as(StepVerifier::create)
                .expectComplete()
                .verify();


    }

}