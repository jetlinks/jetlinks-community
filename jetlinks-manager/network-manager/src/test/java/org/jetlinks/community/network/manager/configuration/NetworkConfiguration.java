package org.jetlinks.community.network.manager.configuration;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.monitor.GatewayServerMetrics;
import org.jetlinks.core.server.monitor.GatewayServerMonitor;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.supports.cluster.EventBusDeviceOperationBroker;
import org.jetlinks.supports.cluster.redis.RedisClusterManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.server.monitor.MicrometerGatewayServerMetrics;
import org.jetlinks.supports.server.session.DefaultDeviceSessionManager;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(JetLinksProperties.class)
public class NetworkConfiguration {
    @Bean
    public VertxOptions vertxOptions() {
        return new VertxOptions();
    }

    @Bean
    public Vertx vertx(VertxOptions vertxOptions) {
        return Vertx.vertx(vertxOptions);
    }

    @Bean
    public EventBus eventBus() {
        return new BrokerEventBus();
    }

    @Bean
    public DeviceRegistry registry() {
        return new InMemoryDeviceRegistry();
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public DefaultDeviceSessionManager deviceSessionManager(JetLinksProperties properties,
                                                            GatewayServerMonitor monitor,
                                                            DeviceRegistry registry) {
        DefaultDeviceSessionManager sessionManager = new DefaultDeviceSessionManager();
        sessionManager.setGatewayServerMonitor(monitor);
        sessionManager.setRegistry(registry);
        Optional.ofNullable(properties.getTransportLimit()).ifPresent(sessionManager::setTransportLimits);

        return sessionManager;
    }

    @Bean
    public GatewayServerMonitor gatewayServerMonitor(JetLinksProperties properties,
                                                     MeterRegistryManager registry) {
        GatewayServerMetrics metrics = new MicrometerGatewayServerMetrics(properties.getServerId(),
            registry.getMeterRegister(DeviceTimeSeriesMetric
                .deviceMetrics().getId()));

        return new GatewayServerMonitor() {
            @Override
            public String getCurrentServerId() {
                return properties.getServerId();
            }

            @Override
            public GatewayServerMetrics metrics() {
                return metrics;
            }
        };
    }

    @Bean(initMethod = "start", destroyMethod = "dispose")
    public EventBusDeviceOperationBroker eventBusDeviceOperationBroker(ClusterManager clusterManager, EventBus eventBus) {
        return new EventBusDeviceOperationBroker(clusterManager.getCurrentServerId(),eventBus);
    }
    @Bean(initMethod = "startup")
    public RedisClusterManager clusterManager(JetLinksProperties properties, ReactiveRedisTemplate<Object, Object> template) {
        return new RedisClusterManager(properties.getClusterName(), properties.getServerId(), template);
    }

    //
}
