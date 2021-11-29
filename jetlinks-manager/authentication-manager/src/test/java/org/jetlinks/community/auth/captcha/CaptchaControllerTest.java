package org.jetlinks.community.auth.captcha;

import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.jetlinks.community.auth.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebFluxTest(CaptchaController.class)
class CaptchaControllerTest extends TestJetLinksController {

    private static final String BASE_URL = "/authorize/captcha";
    @Autowired
    private  CaptchaProperties properties;

    @Autowired
    private ReactiveRedisOperations<String, String> redis;
    @Test
    @Order(1)
    void createCaptcha() {
        client.get()
            .uri(BASE_URL + "/config")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    @Test
    @Order(0)
    void testCreateCaptcha1() {
        client.get()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/image")
                    .queryParam("width",130)
                    .queryParam("height",40)
                    .build()
            )
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void testCreateCaptcha() {
        properties.setEnabled(true);
        client.get()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/image")
                .queryParam("width",130)
                .queryParam("height",40)
                .build()
            )
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void handleAuthEvent() {
        CaptchaProperties captchaProperties = Mockito.mock(CaptchaProperties.class);
        Map<String, Object> map = new HashMap<>();
        map.put("verifyKey","aaa");
        map.put("verifyCode","bbb");
        Function<String, Object> parameterGetter = map::get;
        AuthorizationDecodeEvent event = new AuthorizationDecodeEvent("admin", "admin", parameterGetter);

        CaptchaController captcha = new CaptchaController(captchaProperties,redis);
        Mockito.when(captchaProperties.isEnabled())
            .thenReturn(false);
        captcha.handleAuthEvent(event);
        Mockito.when(captchaProperties.isEnabled())
            .thenReturn(true);
        captcha.handleAuthEvent(event);

    }
}