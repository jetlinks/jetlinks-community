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

import lombok.AllArgsConstructor;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class CompositeCommandSupportManagerProvider implements CommandSupportManagerProvider {
    private final List<CommandSupportManagerProvider> providers;

    @Override
    public String getProvider() {
        return providers.get(0).getProvider();
    }

    @Override
    public Mono<? extends CommandSupport> getCommandSupport(String id, Map<String, Object> options) {

        return Flux
            .fromIterable(providers)
            .flatMap(provider -> provider.getCommandSupport(id, options))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public Flux<CommandSupportInfo> getSupportInfo() {
        return Flux
            .fromIterable(providers)
            .flatMap(CommandSupportManagerProvider::getSupportInfo)
            .distinct(CommandSupportInfo::getId);
    }
}
