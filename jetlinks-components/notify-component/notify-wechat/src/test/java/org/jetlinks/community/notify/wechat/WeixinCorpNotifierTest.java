package org.jetlinks.community.notify.wechat;

import org.jetlinks.core.Values;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class WeixinCorpNotifierTest {


    @Test
    @Disabled
    void test(){
        WechatCorpProperties properties=new WechatCorpProperties();
        properties.setCorpId("corpId");
        properties.setCorpSecret("corpSecret");

        WechatMessageTemplate messageTemplate=new WechatMessageTemplate();

        messageTemplate.setAgentId("agentId");
        messageTemplate.setMessage("test"+System.currentTimeMillis());
        messageTemplate.setToUser("userId");

        WeixinCorpNotifier notifier=new WeixinCorpNotifier("test",
                WebClient.builder().build(),properties,null
        );

        notifier.send(messageTemplate, Values.of(new HashMap<>()))
                .as(StepVerifier::create)
                .expectComplete()
                .verify();

    }

}