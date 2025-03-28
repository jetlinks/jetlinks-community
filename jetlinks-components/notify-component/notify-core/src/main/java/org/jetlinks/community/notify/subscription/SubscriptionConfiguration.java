package org.jetlinks.community.notify.subscription;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionConfiguration {

    @Bean
    public ApplicationContextAware subscriberAutoRegister() {

        return applicationContext -> {
            applicationContext.getBeanProvider(SubscriberProvider.class)
                              .forEach(SubscriberProviders::register);
        };
    }
}
