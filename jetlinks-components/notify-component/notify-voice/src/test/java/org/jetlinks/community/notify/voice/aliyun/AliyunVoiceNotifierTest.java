package org.jetlinks.community.notify.voice.aliyun;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.HttpResponse;
import org.jetlinks.core.Values;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.voice.VoiceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import java.util.HashMap;

/**
 * 输入描述.
 *
 * @author zhangji
 * @version 1.11 2022/2/7
 */
public class AliyunVoiceNotifierTest {
    private IAcsClient          client;
    private AliyunVoiceNotifier notifier;

    private static final String TTS_CODE    = "tts_code";

    @BeforeEach
    void init() throws ClientException {
        client = Mockito.mock(IAcsClient.class);
        CommonResponse response = new CommonResponse();
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setStatus(HttpStatus.OK.value());
        response.setHttpResponse(httpResponse);
        response.setData("{\"Code\":\"ok\"}");
        Mockito.when(client.getCommonResponse(Mockito.any(CommonRequest.class))).thenReturn(response);

        notifier = new AliyunVoiceNotifier(client, null);
    }

    @Test
    void test() {
        Assertions.assertNotNull(notifier);
        Assertions.assertEquals(DefaultNotifyType.voice, notifier.getType());
        Assertions.assertEquals(VoiceProvider.aliyun, notifier.getProvider());
    }

    @Test
    void send() {
        AliyunVoiceTemplate template = new AliyunVoiceTemplate();
        template.setTtsCode(TTS_CODE);
        template.setCalledNumber("calledNumber");

        notifier.send(template, Values.of(new HashMap<>()))
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void close() {
        notifier.close()
            .as(StepVerifier::create)
            .verifyComplete();
    }
}
