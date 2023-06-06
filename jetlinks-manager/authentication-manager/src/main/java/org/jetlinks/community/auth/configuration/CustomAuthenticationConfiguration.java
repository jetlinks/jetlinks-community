package org.jetlinks.community.auth.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.authorization.token.redis.RedisUserTokenManager;
import org.hswebframework.web.authorization.token.redis.SimpleUserToken;
import org.jetlinks.community.auth.enums.UserEntityType;
import org.jetlinks.community.auth.web.WebFluxUserController;
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
@EnableConfigurationProperties({MenuProperties.class})
public class CustomAuthenticationConfiguration {

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

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderAuthCustomizer() {
        return builder -> {
            builder.deserializerByType(UserEntityType.class, new UserEntityTypeJSONDeserializer());
        };
    }
}
