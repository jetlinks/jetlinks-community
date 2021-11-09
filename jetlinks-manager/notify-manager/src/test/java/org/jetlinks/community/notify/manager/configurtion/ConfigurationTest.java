package org.jetlinks.community.notify.manager.configurtion;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.cluster.redis.RedisClusterManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
@EnableConfigurationProperties(JetLinksProperties.class)
public class ConfigurationTest {

    @Bean(initMethod = "startup")
    public RedisClusterManager clusterManager(JetLinksProperties properties, ReactiveRedisTemplate<Object, Object> template) {
        return new RedisClusterManager(properties.getClusterName(), properties.getServerId(), template);
    }

    @Bean
    public EventBus eventBus() {
        return new BrokerEventBus();
    }
}
