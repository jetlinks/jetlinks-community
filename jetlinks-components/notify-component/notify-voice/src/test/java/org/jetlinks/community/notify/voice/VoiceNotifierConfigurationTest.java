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
package org.jetlinks.community.notify.voice;

import org.jetlinks.community.notify.voice.aliyun.AliyunNotifierProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 输入描述.
 *
 * @author zhangji
 * @version 1.11 2022/2/7
 */
public class VoiceNotifierConfigurationTest {
    @Test
    void test() {
        VoiceNotifierConfiguration configuration = new VoiceNotifierConfiguration();
        AliyunNotifierProvider provider = configuration.aliyunNotifierProvider(null);
        Assertions.assertNotNull(provider);
    }
}
