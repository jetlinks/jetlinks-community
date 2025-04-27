package org.jetlinks.community.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.template.AbstractTemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DefaultTemplateManager extends AbstractTemplateManager
    implements SmartInitializingSingleton {

    private final NotifyTemplateService templateService;

    private final ApplicationContext context;

    @Autowired
    public DefaultTemplateManager(EventBus eventBus, NotifyTemplateService templateService, ApplicationContext context) {
        super(eventBus);
        this.templateService = templateService;
        this.context = context;
    }

    @Override
    protected Mono<TemplateProperties> getProperties(NotifyType type, String id) {
        return templateService
            .findById(Mono.just(id))
            .map(NotifyTemplateEntity::toTemplateProperties);
    }

    @Override
    public void afterSingletonsInstantiated() {
        context
            .getBeanProvider(TemplateProvider.class)
            .forEach(this::register);
    }


}
