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
package org.jetlinks.community.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

/**
 * @author bestfeng
 */
@AllArgsConstructor
public class DefaultUserBindService implements UserBindService {

    private final ReactiveRedisOperations<Object, Object> redis;


    @Override
    public Mono<String> generateBindCode(UserInfo userInfo) {
        String code = UserBindService.USER_BIND_CODE_PRE + IDGenerator.MD5.generate();
        return redis
            .opsForValue()
            .set(code, userInfo, Duration.ofMinutes(1))
            .thenReturn(code);
    }


    @Override
    public Mono<UserInfo> getUserInfoByCode(String bindCode) {
        ReactiveValueOperations<Object, Object> operations = redis.opsForValue();
        return operations
            .get(bindCode)
            .cast(UserInfo.class)
            .flatMap(userInfo -> operations.delete(bindCode).thenReturn(userInfo))
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.user_binding_code_incorrect")));
    }


    public void checkUserBind(Authentication authentication, UserInfo userInfo){
        if (!Objects.equals(authentication.getUser().getId(), userInfo.getUserId())){
            throw new BusinessException("error.illegal_bind");
        }
    }


}
