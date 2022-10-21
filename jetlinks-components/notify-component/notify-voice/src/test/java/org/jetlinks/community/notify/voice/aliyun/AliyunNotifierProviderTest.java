package org.jetlinks.community.notify.voice.aliyun;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierProperties;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.voice.VoiceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 输入描述.
 *
 * @author zhangji
 * @version 1.11 2022/2/7
 */
public class AliyunNotifierProviderTest {
    private AliyunNotifierProvider provider;
    private TemplateProperties     templateProperties;
    private NotifierProperties     notifierProperties;

    private static final String TTS_CODE = "ttsCode";
    private static final String NOTIFIER_ID = "notifier_id";

    @BeforeEach
    void init() {
        provider = new AliyunNotifierProvider(null);

        templateProperties = new TemplateProperties();
        templateProperties.setId("test");
        templateProperties.setType(DefaultNotifyType.voice.getId());
        templateProperties.setProvider(VoiceProvider.aliyun.getId());
        AliyunVoiceTemplate aliyunVoiceTemplate = new AliyunVoiceTemplate();
        aliyunVoiceTemplate.setTtsCode(TTS_CODE);
        aliyunVoiceTemplate.setCalledNumber("calledNumber");
        templateProperties.setTemplate((JSONObject)JSONObject.toJSON(aliyunVoiceTemplate));

        notifierProperties = new NotifierProperties();
        notifierProperties.setId(NOTIFIER_ID);
        notifierProperties.setType(DefaultNotifyType.voice.getId());
        notifierProperties.setProvider(VoiceProvider.aliyun.getId());
        Map<String, Object> config = new HashMap<>();
        config.put("regionId", "regionId");
        config.put("accessKeyId", "accessKeyId");
        config.put("secret", "secret");
        notifierProperties.setConfiguration(config);
    }

    @Test
    void test() {
        Assertions.assertEquals(VoiceProvider.aliyun, provider.getProvider());
        Assertions.assertEquals(DefaultNotifyType.voice, provider.getType());
    }

    @Test
    void getTemplateConfigMetadata() {
        ConfigMetadata templateConfig = provider.getTemplateConfigMetadata();
        Assertions.assertNotNull(templateConfig);
        Assertions.assertEquals("阿里云语音模版", templateConfig.getName());
    }

    @Test
    void getNotifierConfigMetadata() {
        ConfigMetadata notifierConfig = provider.getNotifierConfigMetadata();
        Assertions.assertNotNull(notifierConfig);
        Assertions.assertEquals("阿里云通知配置", notifierConfig.getName());
    }

    @Test
    void createTemplate() {
        provider.createTemplate(templateProperties)
            .as(StepVerifier::create)
            .expectNextMatches(template -> template.getTtsCode().equals(TTS_CODE))
            .verifyComplete();
    }

    @Test
    void createNotifier() {
        provider.createNotifier(notifierProperties)
            .as(StepVerifier::create)
            .expectNextMatches(notifier -> notifier.getNotifierId().equals(NOTIFIER_ID))
            .verifyComplete();
    }
}
