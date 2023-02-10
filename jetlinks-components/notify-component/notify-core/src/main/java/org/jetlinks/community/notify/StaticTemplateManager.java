package org.jetlinks.community.notify;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.configuration.StaticNotifyProperties;
import org.jetlinks.community.notify.template.AbstractTemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import reactor.core.publisher.Mono;

@Getter
@Setter
public class StaticTemplateManager extends AbstractTemplateManager implements BeanPostProcessor {

    private StaticNotifyProperties properties;

    public StaticTemplateManager(StaticNotifyProperties properties) {
        this.properties = properties;
    }

    public void register(TemplateProvider provider) {
        super.register(provider);
    }

    @Override
    protected Mono<TemplateProperties> getProperties(NotifyType type, String id) {
        return Mono.justOrEmpty(properties.getTemplateProperties(type, id));
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if(bean instanceof TemplateProvider){
            register(((TemplateProvider) bean));
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
