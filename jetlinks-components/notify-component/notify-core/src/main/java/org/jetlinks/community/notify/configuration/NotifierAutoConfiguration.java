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
package org.jetlinks.community.notify.configuration;

import lombok.Generated;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(StaticNotifyProperties.class)
@Generated
public class NotifierAutoConfiguration {

    @Bean
    public StaticNotifyConfigManager staticNotifyConfigManager(StaticNotifyProperties properties) {
        return new StaticNotifyConfigManager(properties);
    }

    @Bean
    @Primary
    public NotifyConfigManager notifyConfigManager(List<NotifyConfigManager> managers) {
        return new CompositeNotifyConfigManager(managers);
    }


    @Bean
    public StaticTemplateManager staticTemplateManager(StaticNotifyProperties properties,
                                                       EventBus eventBus,
                                                       ApplicationContext context) {
        return new StaticTemplateManager(properties, eventBus, context);
    }

    @Bean
    @Primary
    public TemplateManager templateManager(List<TemplateManager> managers) {
        return new CompositeTemplateManager(managers);
    }


    @Bean
    @ConditionalOnMissingBean(NotifierManager.class)
    public DefaultNotifierManager notifierManager(EventBus eventBus,
                                                  ApplicationContext context,
                                                  NotifyConfigManager configManager) {
        return new DefaultNotifierManager(eventBus, configManager, context);
    }


}
