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
package org.jetlinks.community.plugin.context;

import org.jetlinks.core.command.AsyncProxyCommandSupport;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.plugin.core.ServiceRegistry;
import org.jetlinks.plugin.internal.functional.FunctionalService;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import reactor.core.publisher.Mono;

import java.util.*;

public class CommandServiceRegistry implements ServiceRegistry {

    public static final CommandServiceRegistry INSTANCE = new CommandServiceRegistry();

    public static ServiceRegistry instance() {
        return INSTANCE;
    }

    @Override
    public <T> Optional<T> getService(Class<T> type) {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("all")
    public <T> Optional<T> getService(Class<T> type, String name) {
        //todo 限制可访问的命令服务?
        if ((type == FunctionalService.class || type == CommandSupport.class)) {
            return Optional.of((T) new CommandFunctionalServiceIml(
                CommandSupportManagerProviders
                    .getCommandSupport(name, Collections.emptyMap())));
        }

        return Optional.empty();

    }

    @Override
    public <T> List<T> getServices(Class<T> type) {
        return Collections.emptyList();
    }

    static class CommandFunctionalServiceIml extends AsyncProxyCommandSupport implements FunctionalService {
        public CommandFunctionalServiceIml(Mono<CommandSupport> asyncCommand) {
            super(asyncCommand);
        }
    }

}
