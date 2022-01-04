package org.jetlinks.community.auth.captcha;

import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebFluxTest(CaptchaController.class)
class CaptchaControllerTest extends TestJetLinksController {

    private static final String BASE_URL = "/authorize/captcha";
    @Autowired
    private CaptchaProperties properties;

    @Autowired
    private ReactiveRedisOperations<String, String> redis;
    @Test
    @Order(1)
    void createCaptcha() {
        Flux<CaptchaController.CaptchaConfig> responseBody = client.get()
            .uri(BASE_URL + "/config")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .returnResult(CaptchaController.CaptchaConfig.class)
            .getResponseBody();
        assertNotNull(responseBody);

    }

    @Test
    @Order(0)
    void testCreateCaptcha1() {
        Flux<CaptchaController.CaptchaInfo> responseBody = client.get()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/image")
                    .queryParam("width", 130)
                    .queryParam("height", 40)
                    .build()
            )
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .returnResult(CaptchaController.CaptchaInfo.class)
            .getResponseBody();
        assertNotNull(responseBody);
    }

    @Test
    @Order(2)
    void testCreateCaptcha() {
        properties.setEnabled(true);
        Flux<CaptchaController.CaptchaInfo> responseBody = client.get()
            .uri(uriBuilder ->
                uriBuilder.path(BASE_URL + "/image")
                    .queryParam("width", 130)
                    .queryParam("height", 40)
                    .build()
            )
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .returnResult(CaptchaController.CaptchaInfo.class)
            .getResponseBody();
        assertNotNull(responseBody);
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
        map.put("verifyCode",null);
        Executable executable = ()->captcha.handleAuthEvent(event);
        assertThrows(ValidationException.class,executable);
        map.put("verifyKey",null);
        Executable executable1 = ()->captcha.handleAuthEvent(event);
        assertThrows(ValidationException.class,executable1);

    }
}