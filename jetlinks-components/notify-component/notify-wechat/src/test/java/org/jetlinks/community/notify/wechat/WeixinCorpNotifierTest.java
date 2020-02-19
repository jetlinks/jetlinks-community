package org.jetlinks.community.notify.wechat;

import org.jetlinks.core.Values;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class WeixinCorpNotifierTest {


    @Test
    void test(){
        WechatCorpProperties properties=new WechatCorpProperties();
        properties.setCorpId("wwd7e935e2867897122");
        properties.setCorpSecret("c0qeMSJK2pJee47Bg4kguBmd1JCBt2OFfsNFVGjc1i0");

        WechatMessageTemplate messageTemplate=new WechatMessageTemplate();

        messageTemplate.setAgentId("1000002");
        messageTemplate.setMessage("test"+System.currentTimeMillis());
        messageTemplate.setToUser("zhouhao");

        WeixinCorpNotifier notifier=new WeixinCorpNotifier(
                WebClient.builder().build(),properties,null
        );

        notifier.send(messageTemplate, Values.of(new HashMap<>()))
                .as(StepVerifier::create)
                .expectComplete()
                .verify();

    }

}