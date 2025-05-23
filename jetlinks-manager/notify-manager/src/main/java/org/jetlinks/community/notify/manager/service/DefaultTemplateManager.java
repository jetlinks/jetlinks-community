/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
