package org.jetlinks.community.auth.captcha;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "captcha")
@Getter
@Setter
public class CaptchaProperties {
    private boolean enabled = false;

    private Duration ttl = Duration.ofMinutes(2);

    private CaptchaType type = CaptchaType.image;

    public enum CaptchaType {
        image
    }
}
