package org.jetlinks.community.gateway.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class EventBusDispatcherConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SpringMessageBroker springMessageBroker() {
        return new SpringMessageBroker();
    }
}
