package org.jetlinks.community.auth.login;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.events.AbstractAuthorizationEvent;
import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.hswebframework.web.authorization.events.AuthorizationFailedEvent;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.id.RandomIdGenerator;
import org.hswebframework.web.logging.RequestInfo;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.utils.CryptoUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Authorize(ignore = true)
@RequestMapping
@Tag(name = "登录配置接口")
@Hidden
@AllArgsConstructor
@Slf4j
public class UserLoginLogicInterceptor {

    private final UserLoginProperties properties;

    private final ReactiveRedisOperations<String, String> redis;

    @GetMapping("/authorize/login/configs")
    @Operation(summary = "获取登录所需配置信息")
    public Mono<Map<String, Object>> getConfigs() {

        Map<String, Object> map = new ConcurrentHashMap<>();
        List<Mono<Void>> jobs = new ArrayList<>();

        if (properties.getEncrypt().isEnabled()) {
            jobs.add(getEncryptKey()
                         .doOnNext(enc -> map.put("encrypt", enc))
                         .then());
        } else {
            map.put("encrypt", Collections.singletonMap("enabled", false));
        }
        // TODO: 2023/6/25 其他配置

        return Flux.merge(jobs)
                   .then(Mono.just(map));
    }

    @GetMapping("/authorize/encrypt-key")
    @Operation(summary = "获取登录加密key")
    public Mono<Map<String, Object>> getEncryptKey() {
        if (!properties.getEncrypt().isEnabled()) {
            return Mono.empty();
        }
        String id = IDGenerator.RANDOM.generate();
        KeyPair rasKey = CryptoUtils.generateRSAKey();
        String pubKeyBase64 = Base64.getEncoder().encodeToString(rasKey.getPublic().getEncoded());
        String priKeyBase64 = Base64.getEncoder().encodeToString(rasKey.getPrivate().getEncoded());

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("enabled", true);
        value.put("publicKey", pubKeyBase64);
        value.put("id", id);
        return redis
            .opsForValue()
            .set(createEncRedisKey(id), priKeyBase64, properties.getEncrypt().getKeyTtl())
            .thenReturn(value);
    }

    @EventListener
    public void handleAuthEvent(AuthorizationDecodeEvent event) {
        if (properties.getBlock().isEnabled()) {
            event.async(checkBlocked(event));
        }

        if (properties.getEncrypt().isEnabled()) {
            event.async(Mono.defer(() -> doDecrypt(event)));
        }
    }

    protected boolean isLegalEncryptId(String id) {
        return RandomIdGenerator.timestampRangeOf(id, properties.getEncrypt().getKeyTtl());
    }

    Mono<Void> doDecrypt(AuthorizationDecodeEvent event) {
        String encId = event
            .getParameter("encryptId")
            .map(String::valueOf)
            .filter(this::isLegalEncryptId)
            //统一返回密码错误
            .orElseThrow(() -> new AuthenticationException(AuthenticationException.ILLEGAL_PASSWORD));
        String redisKey = createEncRedisKey(encId);
        return redis
            .opsForValue()
            .get(redisKey)
            .map(privateKey -> {
                event.setPassword(
                    new String(
                        CryptoUtils.decryptRSA(Base64.getDecoder().decode(event.getPassword()),
                                               CryptoUtils.decodeRSAPrivateKey(privateKey))
                    )
                );
                return true;
            })
            .onErrorResume(err -> {
                log.warn("decrypt password error", err);
                return Reactors.ALWAYS_FALSE;
            })
            .defaultIfEmpty(false)
            .flatMap(ignore -> redis.opsForValue().delete(redisKey).thenReturn(ignore))
            .doOnSuccess(success -> {
                if (!success) {
                    throw new AuthenticationException(AuthenticationException.ILLEGAL_PASSWORD);
                }
            })
            .then();

    }

    @EventListener
    public void handleAuthFailed(AuthorizationFailedEvent event) {
        if (event.getException() instanceof AuthenticationException) {
            event.async(recordAuthFailed(event));
        }
    }

    private Mono<Void> recordAuthFailed(AbstractAuthorizationEvent event) {
        if (!properties.getBlock().isEnabled()) {
            return Mono.empty();
        }
        return createBlockRedisKey(event)
            .flatMap(key -> redis
                .opsForValue()
                .increment(key, 1)
                .then(redis.expire(key, properties.getBlock().getTtl())))
            .then();
    }

    private Mono<RequestInfo> currentRequest() {
        return Mono
            .deferContextual(ctx -> Mono.justOrEmpty(ctx.getOrEmpty(RequestInfo.class)));
    }

    private Mono<String> createBlockRedisKey(AbstractAuthorizationEvent event) {
        return currentRequest()
            .map(request -> {
                String hex = DigestUtils.md5Hex(digest -> {
                    if (properties.getBlock().hasScope(UserLoginProperties.BlockLogic.Scope.ip)) {
                        digest.update(properties.getBlock().getRealIp(request.getIpAddr()).getBytes());
                    }
                    if (properties.getBlock().hasScope(UserLoginProperties.BlockLogic.Scope.username)) {
                        digest.update(event.getUsername().trim().getBytes());
                    }
                });
                return "login:blocked:" + hex;
            });
    }

    private Mono<Void> checkBlocked(AuthorizationDecodeEvent event) {

        return this
            .createBlockRedisKey(event)
            .flatMap(key -> redis
                .opsForValue()
                .get(key)
                .doOnNext(l -> {
                    if (CastUtils.castNumber(l).intValue() >= properties.getBlock().getMaximum()) {
                        throw new AccessDenyException("error.user.login.blocked");
                    }
                }))
            .then();
    }


    private static String createEncRedisKey(String encryptId) {
        return "login:encrypt-key:" + encryptId;
    }

}
