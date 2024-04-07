package org.jetlinks.community.protocol.configuration;

import org.jetlinks.community.protocol.*;
import org.jetlinks.community.protocol.local.LocalProtocolSupportLoader;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.configure.device.DeviceClusterConfiguration;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.protocol.*;
import org.jetlinks.supports.protocol.StaticProtocolSupports;
import org.jetlinks.supports.protocol.management.ClusterProtocolSupportManager;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(DeviceClusterConfiguration.class)
public class ProtocolAutoConfiguration {

//    @Bean
//    public ProtocolSupportManager protocolSupportManager(ClusterManager clusterManager) {
//        return new ClusterProtocolSupportManager(clusterManager);
//    }

    @Bean
    public ServiceContext serviceContext(ApplicationContext applicationContext) {
        return new SpringServiceContext(applicationContext);
    }

    @Bean
    public LazyInitManagementProtocolSupports managementProtocolSupports(EventBus eventBus,
                                                                         ClusterManager clusterManager,
                                                                         ProtocolSupportLoader loader) {
        return new LazyInitManagementProtocolSupports(eventBus, clusterManager, loader);
    }

    @Bean
    @Primary
    public LazyProtocolSupports protocolSupports() {
        return new LazyProtocolSupports();
    }

    @Bean
    public AutoDownloadJarProtocolSupportLoader autoDownloadJarProtocolSupportLoader(WebClient.Builder builder, FileManager fileManager) {
        return new AutoDownloadJarProtocolSupportLoader(builder, fileManager);
    }

    @Bean
    public ProtocolSupportLoader protocolSupportLoader(EventBus eventBus,
                                                       ObjectProvider<ProtocolSupportLoaderProvider> providers) {
        SpringProtocolSupportLoader loader = new SpringProtocolSupportLoader(eventBus);
        providers.forEach(loader::register);
        return loader;
    }

    @Bean
    public ProtocolSupports inSpringProtocolSupports(EventBus eventBus,
                                                     ObjectProvider<ProtocolSupport> supports) {
        StaticProtocolSupports protocolSupports = new StaticProtocolSupports();
        for (ProtocolSupport protocol : supports) {
            protocolSupports.register(
                new RenameProtocolSupport(
                    protocol.getId(),
                    protocol.getName(),
                    protocol.getDescription(),
                    protocol,
                    eventBus
                )
            );
        }
        return protocolSupports;
    }

    @Bean
    @Profile("dev")
    public LocalProtocolSupportLoader localProtocolSupportLoader(ServiceContext context) {
        return new LocalProtocolSupportLoader(context);
    }
}
