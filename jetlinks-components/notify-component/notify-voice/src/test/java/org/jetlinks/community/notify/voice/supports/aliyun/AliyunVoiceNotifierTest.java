/*
package org.jetlinks.community.notify.voice.supports.aliyun;

import org.jetlinks.core.Values;
import org.jetlinks.community.notify.NotifierProperties;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.voice.aliyun.AliyunVoiceNotifier;
import org.jetlinks.community.notify.voice.aliyun.AliyunVoiceTemplate;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nonnull;
import java.util.HashMap;

class AliyunVoiceNotifierTest {

    @Test
    void test() {
        NotifierProperties properties = new NotifierProperties();
        properties.setId("test");
        properties.setName("test");
        properties.setProvider("aliyun");
        properties.setConfiguration(new HashMap<String, Object>() {{
            put("regionId", "cn-hangzhou");
            put("accessKeyId", "LTAI4Fj2oYhjnYYMAwTSj1F8");
            put("secret", "tea2nKEKM635IsPDud0OaZ5aIHM8eG");
        }});

        AliyunVoiceNotifier notifier = new AliyunVoiceNotifier(
                properties, new TemplateManager() {
            @Nonnull
            @Override
            public Mono<? extends Template> getTemplate(@Nonnull NotifyType type, @Nonnull String id) {
                return Mono.empty();
            }

            @Nonnull
            @Override
            public Mono<? extends Template> createTemplate(@Nonnull NotifyType type, @Nonnull TemplateProperties properties) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> reload(String templateId) {
                return Mono.empty();
            }
        }
        );
        AliyunVoiceTemplate template = new AliyunVoiceTemplate();
        template.setCalledShowNumbers("02566040637");
        template.setPlayTimes(1);
        template.setTtsCode("TTS_176535661");
        template.setCalledNumber("18502314099");
        notifier.send(template, Values.of(new HashMap<String, Object>() {
            {
                put("busi","测试");
                put("address","测试");

            }
        })).as(StepVerifier::create)
                .verifyComplete();
    }
}*/
