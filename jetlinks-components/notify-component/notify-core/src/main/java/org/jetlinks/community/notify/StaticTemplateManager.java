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
