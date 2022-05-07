package org.jetlinks.community.configure.cluster;

import io.scalecube.cluster.ClusterConfig;
import io.scalecube.net.Address;
import io.scalecube.services.Microservices;
import io.scalecube.services.ServiceInfo;
import io.scalecube.services.ServiceProvider;
import io.scalecube.services.transport.rsocket.RSocketClientTransportFactory;
import io.scalecube.services.transport.rsocket.RSocketServerTransportFactory;
import io.scalecube.services.transport.rsocket.RSocketServiceTransport;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.cluster.redis.RedisClusterManager;
import org.jetlinks.supports.config.EventBusStorageManager;
import org.jetlinks.supports.event.BrokerEventBus;
import org.jetlinks.supports.event.EventBroker;
import org.jetlinks.supports.scalecube.DynamicServiceRegistry;
import org.jetlinks.supports.scalecube.ExtendedCluster;
import org.jetlinks.supports.scalecube.ExtendedClusterImpl;
import org.jetlinks.supports.scalecube.ExtendedServiceDiscoveryImpl;
import org.jetlinks.supports.scalecube.event.ScalecubeEventBusBroker;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.stream.Collectors;

@Configuration
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
    public EventBroker eventBroker(ExtendedCluster cluster) {
        return new ScalecubeEventBusBroker(cluster);
    }

    @Bean
    public BrokerEventBus eventBus(ObjectProvider<EventBroker> provider,
                                   ObjectProvider<Scheduler> scheduler) {

        BrokerEventBus eventBus = new BrokerEventBus();
        eventBus.setPublishScheduler(scheduler.getIfAvailable(Schedulers::parallel));
        for (EventBroker eventBroker : provider) {
            eventBus.addBroker(eventBroker);
        }

        return eventBus;
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

    @Bean
    public Microservices microservices(ExtendedCluster cluster,
                                       ObjectProvider<ServiceInfo> infos,
                                       ObjectProvider<ServiceProvider> providers,
                                       ClusterProperties properties) {
        return Microservices
            .builder()
            .services(infos.stream().toArray())
            .services(call -> providers
                .stream()
                .flatMap(provider -> provider.provide(call).stream())
                .collect(Collectors.toList()))
            .serviceRegistry(new DynamicServiceRegistry())
            .discovery(serviceEndpoint -> new ExtendedServiceDiscoveryImpl(cluster, serviceEndpoint))
            .externalHost(properties.getRpcExternalHost())
            .externalPort(properties.getRpcExternalPort())
            .transport(() -> new RSocketServiceTransport()
                .serverTransportFactory(RSocketServerTransportFactory.tcp(properties.getRpcPort()))
                .clientTransportFactory(RSocketClientTransportFactory.tcp())
            )
            .startAwait();
    }

}
