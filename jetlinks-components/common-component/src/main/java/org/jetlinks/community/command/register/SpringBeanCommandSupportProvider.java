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

import com.google.common.collect.Lists;
import lombok.Getter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.command.CompositeCommandSupport;
import org.jetlinks.community.annotation.command.CommandService;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.supports.command.JavaBeanCommandSupport;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

class SpringBeanCommandSupportProvider implements CommandSupportManagerProvider {

    private final String provider;
    private final Map<String, CompositeSpringBeanCommandSupport> commandSupports = new HashMap<>();

    private final List<CommandSupportManagerProvider> statics = new ArrayList<>();

    public SpringBeanCommandSupportProvider(String provider) {
        this.provider = provider;
    }

    void register(CommandSupportManagerProvider provider) {
        statics.add(provider);
    }

    void register(String support, CommandService annotation, Object bean) {
        Objects.requireNonNull(annotation, "endpoint");
        Objects.requireNonNull(bean, "bean");

        SpringBeanCommandSupport commandSupport = new SpringBeanCommandSupport(annotation, bean);
        if (commandSupport.isEmpty()) {
            return;
        }
        //相同support合并成一个
        commandSupports
            .computeIfAbsent(support, id -> new CompositeSpringBeanCommandSupport(id,provider))
            .register(commandSupport);
    }

    boolean isEmpty() {
        return commandSupports.isEmpty();
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public Mono<? extends CommandSupport> getCommandSupport(String id, Map<String, Object> options) {
        CommandSupport support = commandSupports.get(StringUtils.hasText(id) ? id : provider);
        if (support != null) {
            return Mono.just(support);
        }
        if (statics.isEmpty()) {
            return Mono.empty();
        }
        return Flux
            .fromIterable(statics)
            .flatMap(provider -> provider.getCommandSupport(id, options))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public Flux<CommandSupportInfo> getSupportInfo() {
        if (statics.isEmpty()) {
            return Flux
                .fromIterable(commandSupports.values())
                .flatMapIterable(CompositeSpringBeanCommandSupport::getInfo);
        }
        return Flux
            .concat(
                Flux
                    .fromIterable(commandSupports.values())
                    .flatMapIterable(CompositeSpringBeanCommandSupport::getInfo),
                Flux.fromIterable(statics)
                    .flatMap(CommandSupportManagerProvider::getSupportInfo)
            )
            .distinct(info -> {
                String id = info.getId();
                return String.valueOf(id);
            });
    }

    static class CompositeSpringBeanCommandSupport extends CompositeCommandSupport {
        private final String id;
        private final String provider;

        public CompositeSpringBeanCommandSupport(String id,String provider) {
            super();
            this.id = id;
            this.provider = provider;
        }

        public List<CommandSupportInfo> getInfo() {
            return Lists
                .transform(
                    getSupports(),
                    support -> {
                        SpringBeanCommandSupport commandSupport = support.unwrap(SpringBeanCommandSupport.class);
                        //兼容为null
                        String _id = id.equals(provider) ? null : id;
                        return CommandSupportInfo.of(
                            _id,
                            LocaleUtils.resolveMessage(commandSupport.annotation.name(), commandSupport.annotation.name()),
                            String.join("", commandSupport.annotation.description())
                        );
                    });
        }

        @Override
        public String toString() {
            return getSupports().toString();
        }
    }

    @Getter
    static class SpringBeanCommandSupport extends JavaBeanCommandSupport {
        private final CommandService annotation;

        boolean isEmpty() {
            return handlers.isEmpty();
        }

        public SpringBeanCommandSupport(CommandService annotation, Object target) {
            super(target);
            this.annotation = annotation;
        }

    }
}