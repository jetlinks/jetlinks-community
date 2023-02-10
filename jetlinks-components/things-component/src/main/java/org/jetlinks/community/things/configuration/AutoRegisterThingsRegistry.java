package org.jetlinks.community.things.configuration;

import org.jetlinks.core.things.DefaultThingsRegistry;
import org.jetlinks.core.things.ThingsRegistrySupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.annotation.Nonnull;

public class AutoRegisterThingsRegistry extends DefaultThingsRegistry implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean,@Nonnull String beanName) throws BeansException {
        if(bean instanceof ThingsRegistrySupport){
            addSupport(((ThingsRegistrySupport) bean));
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
