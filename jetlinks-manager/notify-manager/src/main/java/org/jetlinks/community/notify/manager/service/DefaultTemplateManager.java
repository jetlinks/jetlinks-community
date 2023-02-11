package org.jetlinks.community.notify.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.template.AbstractTemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.event.EventBus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DefaultTemplateManager extends AbstractTemplateManager implements BeanPostProcessor {

    private final NotifyTemplateService templateService;

    public DefaultTemplateManager(NotifyTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    protected Mono<TemplateProperties> getProperties(NotifyType type, String id) {
        return templateService
            .findById(Mono.just(id))
            .map(NotifyTemplateEntity::toTemplateProperties);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TemplateProvider) {
            register(((TemplateProvider) bean));
        }
        return bean;
    }
}
