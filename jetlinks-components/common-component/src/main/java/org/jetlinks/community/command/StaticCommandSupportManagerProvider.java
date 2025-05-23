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
package org.jetlinks.community.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.community.annotation.command.CommandService;
import org.springframework.core.annotation.AnnotationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用静态命令管理.
 *
 * @author zhangji 2024/2/2
 * @since 2.2.0
 */
@AllArgsConstructor
public class StaticCommandSupportManagerProvider extends AbstractCommandSupport implements CommandSupportManagerProvider {

    @Getter
    public String provider;

    private final Map<String, CommandSupport> commandSupports = new HashMap<>();

    public void register(String id, CommandSupport commandSupport) {
        commandSupports.put(id, commandSupport);
    }

    @Override
    public final Mono<? extends CommandSupport> getCommandSupport(String id, Map<String, Object> options) {
        CommandSupport cmd = commandSupports.get(id);
        if (cmd == null) {
            return getUndefined(id, options);
        }
        return Mono.just(cmd);
    }

    protected Mono<? extends CommandSupport> getUndefined(String id, Map<String, Object> options) {
        return Mono.just(this);
    }

    @Override
    public Flux<CommandSupportInfo> getSupportInfo() {
        Flux<CommandSupportInfo> another = Flux
            .fromIterable(commandSupports.entrySet())
            .map(entry -> createCommandSupport(entry.getKey(), entry.getValue().getClass()));

        if (!this.handlers.isEmpty()) {
            return Flux.concat(another, Flux.just(createCommandSupport(null, this.getClass())));
        }
        return another;
    }

    protected final CommandSupportInfo createCommandSupport(String id, Class<?> clazz) {
        String name = id;
        String description = null;
        Schema schema = AnnotationUtils.findAnnotation(clazz, Schema.class);
        if (null != schema) {
            name = schema.title();
            description = schema.description();
        }
        CommandService service = AnnotationUtils.findAnnotation(clazz, CommandService.class);
        if (null != service) {
            name = LocaleUtils.resolveMessage(service.name(), service.name());
            description = String.join("", service.description());
        }
        return CommandSupportInfo.of(id, name, description);
    }
}
