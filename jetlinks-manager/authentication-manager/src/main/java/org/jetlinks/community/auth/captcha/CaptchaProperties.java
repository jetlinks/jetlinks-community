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
package org.jetlinks.community.auth.captcha;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.auth.captcha.impl.ImageCaptchaProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 验证码配置
 * <p>
 * 如：
 * <pre>
 *      captcha:
 *          enabled: true # 开启验证码
 *          ttl: 2m #验证码过期时间,2分钟
 * <pre/>
 * </p>
 * @author zhouhao
 * @since 1.4
 */
@Component
@ConfigurationProperties(prefix = "captcha")
@Getter
@Setter
@Generated
public class CaptchaProperties {
    //是否开启验证码
    private boolean enabled = false;

    private Duration ttl = Duration.ofMinutes(2);

    private CaptchaType type = CaptchaType.image;

    public enum CaptchaType {
        image
    }
}
