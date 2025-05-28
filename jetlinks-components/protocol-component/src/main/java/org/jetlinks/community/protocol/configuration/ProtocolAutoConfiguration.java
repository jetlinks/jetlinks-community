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
package org.jetlinks.community.protocol.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.jetlinks.community.protocol.*;
import org.jetlinks.community.protocol.local.LocalProtocolSupportLoader;
import org.jetlinks.community.protocol.manager.LocalProtocolSupportManager;
import org.jetlinks.community.protocol.monitor.ProtocolMonitorHelper;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.configure.device.DeviceClusterConfiguration;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.supports.protocol.StaticProtocolSupports;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
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
@EnableEasyormRepository("org.jetlinks.community.protocol.ProtocolSupportEntity")
public class ProtocolAutoConfiguration {

    @Bean
    public ServiceContext serviceContext(ApplicationContext applicationContext) {
        return new SpringServiceContext(applicationContext);
    }

    @Bean
    public LocalProtocolSupportManager protocolSupportManager(DataReferenceManager referenceManager,
                                                                   ProtocolSupportLoader loader,
                                                                   ReactiveRepository<ProtocolSupportEntity, String> repository) {
        return new LocalProtocolSupportManager(referenceManager, loader, repository);
    }

    @Bean
    @Primary
    public LazyProtocolSupports protocolSupports() {
        return new LazyProtocolSupports();
    }


    @Bean
    public ProtocolMonitorHelper protocolMonitorHelper(EventBus eventBus) {
        return new ProtocolMonitorHelper(eventBus);
    }

    @Bean
    public AutoDownloadJarProtocolSupportLoader autoDownloadJarProtocolSupportLoader(WebClient.Builder builder,
                                                                                     FileManager fileManager,
                                                                                     ProtocolMonitorHelper monitorHelper) {
        return new AutoDownloadJarProtocolSupportLoader(builder, fileManager, monitorHelper);
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
    public LocalProtocolSupportLoader localProtocolSupportLoader(ServiceContext context,
                                                                 ProtocolMonitorHelper helper) {
        return new LocalProtocolSupportLoader(context, helper);
    }
}
