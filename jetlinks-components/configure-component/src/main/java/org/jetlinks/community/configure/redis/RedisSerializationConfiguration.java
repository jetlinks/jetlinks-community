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
package org.jetlinks.community.configure.redis;

import io.lettuce.core.ClientOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(prefix = "spring.redis",name = "serializer",havingValue = "obj",matchIfMissing = true)
public class RedisSerializationConfiguration {

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return builder -> builder
            .clientOptions(
                builder
                    .build()
                    .getClientOptions()
                    .orElseGet(ClientOptions::create)
                    .mutate()
                    .publishOnScheduler(true)
                    .build()
            );
    }


    @Bean
    @Primary
    public ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        ObjectRedisSerializer serializer = new ObjectRedisSerializer();
        @SuppressWarnings("all")
        RedisSerializationContext<Object, Object> serializationContext = RedisSerializationContext
            .newSerializationContext()
            .key((RedisSerializer) StringRedisSerializer.UTF_8)
            .value(serializer)
            .hashKey(StringRedisSerializer.UTF_8)
            .hashValue(serializer)
            .build();

        return new PublishOnReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

}
