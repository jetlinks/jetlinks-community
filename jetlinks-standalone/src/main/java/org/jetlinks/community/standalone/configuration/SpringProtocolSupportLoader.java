package org.jetlinks.community.standalone.configuration;

import org.jetlinks.supports.protocol.management.MuiltiProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class SpringProtocolSupportLoader extends MuiltiProtocolSupportLoader implements BeanPostProcessor {


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof ProtocolSupportLoaderProvider){
            register(((ProtocolSupportLoaderProvider) bean));
        }
        return bean;
    }
}
