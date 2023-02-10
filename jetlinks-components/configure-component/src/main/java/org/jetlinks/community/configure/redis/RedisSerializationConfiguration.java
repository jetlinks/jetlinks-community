package org.jetlinks.community.configure.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

}
