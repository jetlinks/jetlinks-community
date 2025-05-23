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
package org.jetlinks.community.auth.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.authorization.token.redis.RedisUserTokenManager;
import org.hswebframework.web.authorization.token.redis.SimpleUserToken;
import org.jetlinks.community.auth.dimension.UserAuthenticationEventPublisher;
import org.jetlinks.community.auth.enums.UserEntityType;
import org.jetlinks.community.auth.web.WebFluxUserController;
import org.jetlinks.core.event.EventBus;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisOperations;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
    MenuProperties.class,
    AuthorizationProperties.class
})
public class CustomAuthenticationConfiguration {

    static final String CONDITION_CLASS_NAME = "org.jetlinks.community.microservice.configuration.CloudServicesConfiguration";

    @Bean
    @Primary
    public WebFluxUserController webFluxUserController() {
        return new WebFluxUserController();
    }

    @Bean
    @ConfigurationProperties(prefix = "hsweb.user-token")
    public UserTokenManager userTokenManager(ReactiveRedisOperations<Object, Object> template,
                                             ApplicationEventPublisher eventPublisher) {
        RedisUserTokenManager userTokenManager = new RedisUserTokenManager(template);
        userTokenManager.setLocalCache(Caffeine
                                           .newBuilder()
                                           .expireAfterAccess(Duration.ofMinutes(10))
                                           .expireAfterWrite(Duration.ofHours(2))
                                           .<String, SimpleUserToken>build()
                                           .asMap());
        userTokenManager.setEventPublisher(eventPublisher);
        return userTokenManager;
    }

    @Bean(destroyMethod = "shutdown")
    public UserAuthenticationEventPublisher userDimensionEventPublisher(EventBus eventBus) {
        return new UserAuthenticationEventPublisher(eventBus);
    }

    @Bean
    public AuthorizationPermissionInitializeService authorizationPermissionInitializeService(AuthorizationProperties properties){
        return new AuthorizationPermissionInitializeService(properties);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderAuthCustomizer() {
        return builder -> {
            builder.deserializerByType(UserEntityType.class, new UserEntityTypeJSONDeserializer());
        };
    }
}
