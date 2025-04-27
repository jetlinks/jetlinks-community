package org.jetlinks.community.things.configuration;

import org.jetlinks.core.things.DefaultThingsRegistry;
import org.jetlinks.core.things.ThingsRegistrySupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nonnull;

public class AutoRegisterThingsRegistry extends DefaultThingsRegistry
    implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void afterSingletonsInstantiated() {
        if (context != null) {
            context.getBeanProvider(ThingsRegistrySupport.class)
                   .forEach(this::addSupport);
        }
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
