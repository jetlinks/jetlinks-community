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
package org.jetlinks.community.things.configuration;

import lombok.Generated;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.things.ThingsDataProperties;
import org.jetlinks.community.things.data.*;
import org.jetlinks.community.things.holder.ThingsRegistryHolderInitializer;
import org.jetlinks.community.things.impl.entity.PropertyMetricEntity;
import org.jetlinks.community.things.impl.metric.DefaultPropertyMetricManager;
import org.jetlinks.core.defaults.DeviceThingsRegistrySupport;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.things.ThingsRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@AutoConfiguration
@Generated
@EnableConfigurationProperties(ThingsDataProperties.class)
public class ThingsConfiguration {

    @Bean
    public NoneThingsDataRepositoryStrategy noneThingsDataRepositoryStrategy() {
        return new NoneThingsDataRepositoryStrategy();
    }

    @Bean(destroyMethod = "shutdown")
    public AutoUpdateThingsDataManager thingsDataManager(EventBus eventBus) {
        String fileName = "./data/things-property/data";
        return new AutoUpdateThingsDataManager(fileName, eventBus);
    }

    @Bean
    public DefaultPropertyMetricManager propertyMetricManager(ThingsRegistry registry,
                                                              EventBus eventBus,
                                                              @SuppressWarnings("all")
                                                              ReactiveRepository<PropertyMetricEntity, String> repository) {
        return new DefaultPropertyMetricManager(registry, eventBus, repository);
    }

    @Bean
    @ConditionalOnBean(DeviceRegistry.class)
    public DeviceThingsRegistrySupport deviceThingsRegistrySupport(DeviceRegistry registry) {
        return new DeviceThingsRegistrySupport(registry);
    }

    @Bean
    public DefaultThingsDataRepository thingDataService(ThingsRegistry registry,
                                                        ObjectProvider<ThingsDataCustomizer> customizers,
                                                        ObjectProvider<ThingsDataRepositoryStrategy> policies) {
        DefaultThingsDataRepository service = new DefaultThingsDataRepository(registry);
        policies.forEach(service::addPolicy);

        for (ThingsDataCustomizer customizer : customizers) {
            customizer.custom(service);
        }
        return service;
    }

    @Bean
    @Primary
    public AutoRegisterThingsRegistry thingsRegistry() {
        AutoRegisterThingsRegistry registry = new AutoRegisterThingsRegistry();
        ThingsRegistryHolderInitializer.init(registry);
        return registry;
    }
}
