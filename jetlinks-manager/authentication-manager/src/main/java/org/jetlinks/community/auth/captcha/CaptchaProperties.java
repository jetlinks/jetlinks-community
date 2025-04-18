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
