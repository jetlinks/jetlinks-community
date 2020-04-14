package org.jetlinks.community.standalone.configuration;

import org.jetlinks.community.standalone.configuration.fst.FstSerializationRedisSerializer;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(prefix = "spring.redis",name = "serializer",havingValue = "fst")
public class JetLinksRedisConfiguration {

    @Bean
    public ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate(
        ReactiveRedisConnectionFactory reactiveRedisConnectionFactory, ResourceLoader resourceLoader) {

        FstSerializationRedisSerializer serializer = new FstSerializationRedisSerializer(() -> {
            FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration()
                .setForceSerializable(true);
            configuration.setClassLoader(resourceLoader.getClassLoader());
            return configuration;
        });
        @SuppressWarnings("all")
        RedisSerializationContext<Object, Object> serializationContext = RedisSerializationContext
            .newSerializationContext()
            .key((RedisSerializer)new StringRedisSerializer())
            .value(serializer)
            .hashKey(StringRedisSerializer.UTF_8)
            .hashValue(serializer)
            .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
    }

}
