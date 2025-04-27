package org.jetlinks.community.protocol.configuration;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.defaults.CompositeProtocolSupports;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class LazyProtocolSupports extends CompositeProtocolSupports
    implements SmartInitializingSingleton, ApplicationContextAware {


    private ApplicationContext applicationContext;


    @Override
    public void afterSingletonsInstantiated() {
        for (ProtocolSupports value : applicationContext
            .getBeansOfType(ProtocolSupports.class)
            .values()) {
            if (value == this) {
                continue;
            }
            register(value);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

    }
}
