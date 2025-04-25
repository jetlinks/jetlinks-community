package org.jetlinks.community.notify.manager.configuration;

import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProviders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SubscriptionConfiguration {

    @Bean
    public ApplicationContextAware subscriberAutoRegister(ObjectProvider<SubscriberProvider> providers) {

        return applicationContext -> {
            applicationContext.getBeanProvider(SubscriberProvider.class)
                              .forEach(SubscriberProviders::register);
        };
    }


}
