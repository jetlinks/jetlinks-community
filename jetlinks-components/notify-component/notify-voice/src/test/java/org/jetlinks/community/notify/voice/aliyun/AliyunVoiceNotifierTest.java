/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
