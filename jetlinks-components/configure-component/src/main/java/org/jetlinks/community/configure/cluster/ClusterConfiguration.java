package org.jetlinks.community.configure.cluster;

import io.scalecube.cluster.ClusterConfig;
import io.scalecube.net.Address;
import io.scalecube.services.transport.rsocket.RSocketClientTransportFactory;
import io.scalecube.services.transport.rsocket.RSocketServerTransportFactory;
import io.scalecube.services.transport.rsocket.RSocketServiceTransport;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.cluster.redis.RedisClusterManager;
import org.jetlinks.supports.config.EventBusStorageManager;
import org.jetlinks.supports.event.InternalEventBus;
import org.jetlinks.supports.scalecube.ExtendedCluster;
import org.jetlinks.supports.scalecube.ExtendedClusterImpl;
import org.jetlinks.supports.scalecube.rpc.ScalecubeRpcManager;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClusterProperties.class)
@ConditionalOnClass(ExtendedCluster.class)
public class ClusterConfiguration {

    @Bean
    public ExtendedClusterImpl cluster(ClusterProperties properties, ResourceLoader resourceLoader) {

        FSTMessageCodec codec = new FSTMessageCodec(() -> {
            FSTConfiguration configuration = FSTConfiguration
                .createDefaultConfiguration()
                .setForceSerializable(true);

            configuration.setClassLoader(resourceLoader.getClassLoader());
            return configuration;
        });

        ExtendedClusterImpl impl = new ExtendedClusterImpl(
            new ClusterConfig()
                .transport(conf -> conf
                    .port(properties.getPort())
                    .messageCodec(codec)
                    .transportFactory(new TcpTransportFactory()))
                .memberAlias(properties.getId())
                .externalHost(properties.getExternalHost())
                .externalPort(properties.getExternalPort())
                .membership(conf -> conf
                    .seedMembers(properties
                                     .getSeeds()
                                     .stream()
                                     .map(Address::from)
                                     .collect(Collectors.toList()))


                )

        );
        impl.startAwait();
        return impl;
    }

    @Bean
    public InternalEventBus eventBus() {
        return new InternalEventBus();
    }

    @Bean
    public EventBusStorageManager eventBusStorageManager(ClusterManager clusterManager, EventBus eventBus) {
        return new EventBusStorageManager(clusterManager,
                                          eventBus,
                                          -1);
    }

    @Bean(initMethod = "startup")
    public RedisClusterManager clusterManager(ClusterProperties properties, ReactiveRedisTemplate<Object, Object> template) {
        return new RedisClusterManager(properties.getName(), properties.getId(), template);
    }

    @Bean(initMethod = "startAwait", destroyMethod = "stopAwait")
    public ScalecubeRpcManager rpcManager(ExtendedCluster cluster, ClusterProperties properties) {
        return new ScalecubeRpcManager(cluster,
                                       () -> new RSocketServiceTransport()
                                           .serverTransportFactory(RSocketServerTransportFactory.tcp(properties.getRpcPort()))
                                           .clientTransportFactory(RSocketClientTransportFactory.tcp()))
            .externalHost(properties.getRpcExternalHost())
            .externalPort(properties.getRpcExternalPort());
    }

}
