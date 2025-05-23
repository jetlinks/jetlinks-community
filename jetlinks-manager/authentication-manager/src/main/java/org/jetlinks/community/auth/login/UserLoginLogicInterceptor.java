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
package org.jetlinks.community.auth.login;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.events.AbstractAuthorizationEvent;
import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.hswebframework.web.authorization.events.AuthorizationFailedEvent;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.hswebframework.web.logging.RequestInfo;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.auth.cipher.CipherConfig;
import org.jetlinks.community.auth.cipher.CipherHelper;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Authorize(ignore = true)
@RequestMapping
@Tag(name = "登录配置接口")
@Hidden
@Slf4j
public class UserLoginLogicInterceptor {

    private final UserLoginProperties properties;
    private final CipherHelper cipherHelper;
    private final ReactiveRedisOperations<String, String> redis;

    public UserLoginLogicInterceptor(UserLoginProperties properties, ReactiveRedisOperations<String, String> redis) {
        this.properties = properties;
        this.redis = redis;
        this.cipherHelper = new CipherHelper(redis, properties.getEncrypt());
    }

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
    public Mono<CipherConfig> getEncryptKey() {
        if (!properties.getEncrypt().isEnabled()) {
            return Mono.empty();
        }
        return cipherHelper.getConfig();
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


    Mono<Void> doDecrypt(AuthorizationDecodeEvent event) {
        String encId = event
            .getParameter("encryptId")
            .map(String::valueOf)
            //统一返回密码错误
            .orElseThrow(() -> new AuthenticationException.NoStackTrace(AuthenticationException.ILLEGAL_PASSWORD));

        return cipherHelper
            .decrypt(encId, event.getPassword())
            .doOnNext(event::setPassword)
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

}
