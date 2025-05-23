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
package org.jetlinks.community.auth.captcha.impl;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.id.RandomIdGenerator;
import org.jetlinks.community.auth.captcha.CaptchaProvider;
import org.jetlinks.community.auth.captcha.ValidationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;

@Getter
@Setter
@RestController
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "captcha.image")
@ConditionalOnProperty(prefix = "captcha.image", name = "enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "图片验证码接口")
public class ImageCaptchaProvider implements CaptchaProvider {

    public static final String provider = "image";

    private final ReactiveRedisOperations<String, String> redis;

    private Duration ttl = Duration.ofMinutes(2);

    private boolean enabled = true;

    private int charType = Captcha.TYPE_DEFAULT;

    private int length = 4;

    @Override
    public String getType() {
        return "image";
    }

    @Override
    public Object getConfigForFront() {
        return Collections.singletonMap(
            "imageUrl","/authorize/captcha/image"
        );
    }

    protected boolean isLegalKey(String key) {
        return RandomIdGenerator.timestampRangeOf(key, ttl);
    }

    @Override
    public Mono<Void> validate(ValidationContext event) {
        if (!enabled) {
            return Mono.empty();
        }

        String key = event
            .getParameter("verifyKey")
            .map(String::valueOf)
            .filter(this::isLegalKey)
            .orElseThrow(() -> new ValidationException.NoStackTrace("error.verification_code_expired"));

        String code = event
            .getParameter("verifyCode")
            .map(String::valueOf)
            .orElseThrow(() -> new ValidationException.NoStackTrace("error.verification_code"));

        String redisKey = "captcha:" + key;
        return redis
            .opsForValue()
            .get(redisKey)
            .map(code::equalsIgnoreCase)
            .defaultIfEmpty(false)
            .flatMap(checked -> redis
                .delete(redisKey)
                .then(checked ? Mono.empty() : Mono.error(new ValidationException.NoStackTrace("error.verification_code"))));
    }

    @GetMapping("/authorize/captcha/image")
    @Operation(summary = "获取验证码图片")
    public Mono<CaptchaInfo> createCaptcha(@RequestParam(defaultValue = "130")
                                           @Parameter(description = "宽度,默认130px") int width,
                                           @RequestParam(defaultValue = "40")
                                           @Parameter(description = "高度,默认40px") int height) {
        if (!enabled) {
            return Mono.empty();
        }
        SpecCaptcha captcha = new SpecCaptcha(width, height, length);
        captcha.setCharType(charType);

        String base64 = captcha.toBase64();
        String key = IDGenerator.RANDOM.generate();
        return redis
            .opsForValue()
            .set("captcha:" + key, captcha.text(), getTtl())
            .thenReturn(new CaptchaInfo(key, base64));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CaptchaInfo {
        @Schema(description = "验证码标识,登录时需要在参数[verifyKey]传入此值.")
        private String key;

        @Schema(description = "图片Base64,以data:image/png;base64,开头")
        private String base64;
    }
}
