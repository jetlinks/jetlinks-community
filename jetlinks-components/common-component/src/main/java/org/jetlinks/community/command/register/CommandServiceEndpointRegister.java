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
package org.jetlinks.community.command.register;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import org.jetlinks.community.command.CompositeCommandSupportManagerProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.jetlinks.community.annotation.command.CommandService;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CommandServiceEndpointRegister implements ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = context.getBeansWithAnnotation(CommandService.class);

        //静态Provider
        Map<String, List<CommandSupportManagerProvider>> statics = context
            .getBeanProvider(CommandSupportManagerProvider.class)
            .stream()
            .collect(Collectors.groupingBy(CommandSupportManagerProvider::getProvider));

        Map<String, SpringBeanCommandSupportProvider> providers = new HashMap<>();

        for (Object value : beans.values()) {
            CommandService endpoint =
                AnnotatedElementUtils.findMergedAnnotation(ClassUtils.getUserClass(value), CommandService.class);
            if (endpoint == null || !endpoint.autoRegistered()) {
                continue;
            }
            String id = endpoint.id();
            String support = id;
            if (id.contains(":")) {
                support = id.substring(id.indexOf(":") + 1);
                id = id.substring(0, id.indexOf(":"));
            }

            SpringBeanCommandSupportProvider provider = providers
                .computeIfAbsent(id, SpringBeanCommandSupportProvider::new);
            log.debug("register command support:{} -> {}", endpoint.id(), value);
            provider.register(support, endpoint, value);
        }

        for (SpringBeanCommandSupportProvider value : providers.values()) {
            if (value.isEmpty()) {
                continue;
            }
            //合并静态Provider
            List<CommandSupportManagerProvider> provider = statics.remove(value.getProvider());
            if (provider != null) {
                provider.forEach(value::register);
            }

            CommandSupportManagerProviders.register(value);
        }
        for (List<CommandSupportManagerProvider> value : statics.values()) {
            if (value.size() == 1) {
                CommandSupportManagerProviders.register(value.get(0));
            } else {
                CommandSupportManagerProviders.register(new CompositeCommandSupportManagerProvider(value));
            }
        }
    }

}
