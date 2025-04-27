package org.jetlinks.community.notify;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.configuration.StaticNotifyProperties;
import org.jetlinks.community.notify.template.AbstractTemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

@Getter
@Setter
public class StaticTemplateManager extends AbstractTemplateManager implements SmartInitializingSingleton {

    private StaticNotifyProperties properties;

    private final ApplicationContext context;

    public StaticTemplateManager(StaticNotifyProperties properties, EventBus eventBus,
                                 ApplicationContext context) {
        super(eventBus);
        this.properties = properties;
        this.context = context;
    }

    public void register(TemplateProvider provider) {
        super.register(provider);
    }

    @Override
    protected Mono<TemplateProperties> getProperties(NotifyType type, String id) {
        return Mono.justOrEmpty(properties.getTemplateProperties(type, id));
    }

    @Override
    public void afterSingletonsInstantiated() {
         context.getBeanProvider(TemplateProvider.class)
             .forEach(this::register);
    }
}
