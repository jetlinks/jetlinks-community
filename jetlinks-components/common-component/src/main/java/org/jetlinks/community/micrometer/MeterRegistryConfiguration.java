package org.jetlinks.community.micrometer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.stream.Collectors;

@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MeterRegistryConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public MeterRegistryManager meterRegistryManager(ObjectProvider<MeterRegistrySupplier> registrySuppliers,
                                                     ObjectProvider<MeterRegistryCustomizer> customizers) {
        return new MeterRegistryManager(registrySuppliers.stream().collect(Collectors.toList()),
                                        customizers.stream().collect(Collectors.toList()));
    }

}
