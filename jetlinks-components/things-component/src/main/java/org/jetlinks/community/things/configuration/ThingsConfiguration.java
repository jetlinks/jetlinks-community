package org.jetlinks.community.things.configuration;

import lombok.Generated;
import org.jetlinks.community.things.data.*;
import org.jetlinks.core.defaults.DeviceThingsRegistrySupport;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.things.ThingsRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@AutoConfiguration
@Generated
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
    @Primary
    public AutoRegisterThingsRegistry thingsRegistry() {
        return new AutoRegisterThingsRegistry();
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
}
