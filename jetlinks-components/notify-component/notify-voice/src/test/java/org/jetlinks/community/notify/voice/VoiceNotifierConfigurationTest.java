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
