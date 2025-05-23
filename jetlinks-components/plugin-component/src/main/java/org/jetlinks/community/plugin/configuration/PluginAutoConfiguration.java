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
package org.jetlinks.community.plugin.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.jetlinks.community.plugin.impl.*;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.plugin.core.PluginRegistry;
import org.jetlinks.plugin.core.ServiceRegistry;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.device.PluginDeviceGatewayService;
import org.jetlinks.plugin.internal.device.PluginDeviceManager;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.plugin.PluginDriverInstaller;
import org.jetlinks.community.plugin.PluginDriverManager;
import org.jetlinks.community.plugin.context.SpringServiceRegistry;
import org.jetlinks.community.plugin.device.DefaultPluginDeviceManager;
import org.jetlinks.community.plugin.device.PluginDeviceGatewayProvider;
import org.jetlinks.community.plugin.device.PluginDeviceGatewayServiceImpl;
import org.jetlinks.community.plugin.impl.id.DefaultPluginDataIdMapper;
import org.jetlinks.community.plugin.impl.id.PluginDataIdMappingEntity;
import org.jetlinks.community.plugin.impl.jar.JarPluginDriverInstallerProvider;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableEasyormRepository(
    {
        "org.jetlinks.community.plugin.impl.PluginDriverEntity",
        "org.jetlinks.community.plugin.impl.id.PluginDataIdMappingEntity",
        "org.jetlinks.community.plugin.impl.standalone.StandalonePluginEntity"
    })
public class PluginAutoConfiguration {

    @Bean
    public DefaultPluginDataIdMapper pluginDataIdMapper(ReactiveRepository<PluginDataIdMappingEntity, String> repository,
                                                        ConfigStorageManager storageManager) {
        return new DefaultPluginDataIdMapper(repository, storageManager);
    }

    @Bean
    public JarPluginDriverInstallerProvider jarPluginDriverLoaderProvider(WebClient.Builder builder) {
        return new JarPluginDriverInstallerProvider(builder);
    }

    @Bean
    @Primary
    public DefaultPluginDriverInstaller pluginDriverLoader(ObjectProvider<PluginDriverInstallerProvider> loaders) {
        DefaultPluginDriverInstaller loader = new DefaultPluginDriverInstaller();
        loaders.forEach(loader::addProvider);
        return loader;
    }

    @Bean
    public SpringServiceRegistry springServiceRegistry(ApplicationContext context) {
        return new SpringServiceRegistry(context);
    }

    @Bean
    public DefaultPluginDriverManager pluginDriverManager(ReactiveRepository<PluginDriverEntity, String> repository,
                                                          PluginDriverInstaller loader) {
        return new DefaultPluginDriverManager(repository, loader);
    }


    @Bean
    @ConditionalOnMissingBean(PluginRegistry.class)
    public NonePluginRegistry nonePluginRegistry() {
        return new NonePluginRegistry();
    }

    @Bean
    public PluginI18nMessageSource pluginI18nMessageSource(PluginDriverManager driverManager) {
        PluginI18nMessageSource messageSource = new PluginI18nMessageSource();
        driverManager.listen(messageSource);
        return messageSource;
    }

    @AutoConfiguration
    @ConditionalOnProperty(prefix = "jetlinks.device.gateway.plugin", value = "enabled", havingValue = "true", matchIfMissing = true)
    static class DevicePluginAutoConfiguration {

        @Bean
        @ConditionalOnBean(DeviceGatewayManager.class)
        public PluginDriverHandler pluginDriverHandler(DeviceGatewayManager deviceGatewayManager,
                                                       DataReferenceManager referenceManager,
                                                       PluginDriverManager driverManager) {
            return new PluginDriverHandler(deviceGatewayManager, referenceManager, driverManager);
        }

        @Bean
        @ConditionalOnBean({
            DeviceSessionManager.class,
            DecodedClientMessageHandler.class
        })
        @ConditionalOnMissingBean(PluginDeviceGatewayService.class)
        public PluginDeviceGatewayServiceImpl pluginDeviceGatewayService(
            DeviceRegistry registry,
            DeviceSessionManager sessionManager,
            DecodedClientMessageHandler handler,
            PluginDataIdMapper dataIdMapper) {

            return new PluginDeviceGatewayServiceImpl(registry, sessionManager, handler, dataIdMapper);
        }

        @Bean
        @ConditionalOnBean(PluginDeviceGatewayService.class)
        public PluginDeviceGatewayProvider pluginDeviceGatewayProvider(PluginRegistry registry,
                                                                       PluginDriverManager driverManager,
                                                                       ServiceRegistry serviceRegistry,
                                                                       PluginDataIdMapper idMapper,
                                                                       DeviceRegistry deviceRegistry,
                                                                       PluginDeviceGatewayService gatewayService,
                                                                       EventBus eventBus) {
            return new PluginDeviceGatewayProvider(registry,
                                                   driverManager,
                                                   serviceRegistry,
                                                   idMapper,
                                                   deviceRegistry,
                                                   gatewayService,
                                                   eventBus);
        }

        @Bean
        @ConditionalOnBean(DeviceRegistry.class)
        @ConditionalOnMissingBean(PluginDeviceManager.class)
        public DefaultPluginDeviceManager pluginDeviceManager(DeviceRegistry registry,
                                                              PluginDataIdMapper idMapper) {
            return new DefaultPluginDeviceManager(registry, idMapper);
        }

    }


}