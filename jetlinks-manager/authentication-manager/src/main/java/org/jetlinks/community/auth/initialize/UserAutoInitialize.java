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
package org.jetlinks.community.auth.initialize;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "jetlinks.user-init",
    name = "enabled",
    havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class UserAutoInitialize implements CommandLineRunner {

    private final UserAutoInitializeProperties properties;

    private final ReactiveUserService userService;

    @Override
    public void run(String... args) throws Exception {
        if (properties.isEnabled() && CollectionUtils.isNotEmpty(properties.getUsers())) {
            Map<String, UserEntity> mapping = properties.getUsers()
                .stream()
                .filter(user -> StringUtils.hasText(user.getUsername()))
                .collect(Collectors.toMap(UserEntity::getUsername, user -> user));
            QueryParamEntity
                .newQuery()
                .in(UserEntity::getUsername, mapping.keySet())
                .execute(userService::findUser)
                .doOnNext(u -> mapping.remove(u.getUsername()))
                .then(
                    Mono.defer(() -> Flux
                        .fromIterable(mapping.values())
                        .flatMap(user -> initAdminUser(user)
                            .onErrorResume(err -> {
                                log.error("init user [{}] error", user.getUsername(), err);
                                return Mono.empty();
                            }))
                        .then())
                )
                .subscribe();

        }
    }

    private Mono<UserEntity> initAdminUser(UserEntity entity) {
        entity.setCreatorId("system");
        if (entity.getName() == null) {
            entity.setName(entity.getUsername());
        }

        return userService
            .saveUser(Mono.just(entity))
            .thenReturn(entity);
    }
}
