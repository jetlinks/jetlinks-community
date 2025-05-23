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
package org.jetlinks.community.auth.cipher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.id.RandomIdGenerator;
import org.jetlinks.community.utils.CryptoUtils;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.util.Base64;

@Slf4j
@AllArgsConstructor
public class CipherHelper {

    private final ReactiveRedisOperations<String, String> redis;

    private final CipherProperties properties;

    private String createEncRedisKey(String encryptId) {
        return "cipher:encrypt-key:" + encryptId;
    }


    public boolean isEnabled(){
       return properties.isEnabled();
    }

    public Mono<CipherConfig> getConfig() {
        if (!isEnabled()) {
            CipherConfig config = new CipherConfig();
            config.setEnabled(false);
            return Mono.just(config);
        }
        String id = IDGenerator.RANDOM.generate();
        KeyPair rasKey = CryptoUtils.generateRSAKey();
        String pubKeyBase64 = Base64.getEncoder().encodeToString(rasKey.getPublic().getEncoded());
        String priKeyBase64 = Base64.getEncoder().encodeToString(rasKey.getPrivate().getEncoded());

        CipherConfig value = new CipherConfig();
        value.setEnabled(true);
        value.setPublicKey(pubKeyBase64);
        value.setId(id);
        return redis
            .opsForValue()
            .set(createEncRedisKey(id), priKeyBase64, properties.getKeyTtl())
            .thenReturn(value);
    }


    public Mono<String> decrypt(String encId, String text) {
        if (!isLegalEncryptId(encId)) {
            return Mono.error(() -> new AuthenticationException.NoStackTrace(AuthenticationException.ILLEGAL_PASSWORD));
        }
        String redisKey = createEncRedisKey(encId);
        return redis
            .opsForValue()
            .get(redisKey)
            .map(privateKey -> new String(
                CryptoUtils.decryptRSA(Base64.getDecoder().decode(text),
                                       CryptoUtils.decodeRSAPrivateKey(privateKey))
            ))
            .onErrorResume(err -> {
                log.warn("decrypt password error", err);
                return redis
                    .opsForValue()
                    .delete(redisKey)
                    .then(Mono.error(() -> new AuthenticationException.NoStackTrace(AuthenticationException.ILLEGAL_PASSWORD)));
            })
            .flatMap(s -> redis
                .opsForValue()
                .delete(redisKey)
                .thenReturn(s));
    }

    private boolean isLegalEncryptId(String id) {
        return RandomIdGenerator.timestampRangeOf(id, properties.getKeyTtl());
    }
}
