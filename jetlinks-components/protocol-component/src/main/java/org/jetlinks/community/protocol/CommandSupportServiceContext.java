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
package org.jetlinks.community.protocol;

import lombok.AllArgsConstructor;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import org.jetlinks.core.Value;
import org.jetlinks.core.command.AsyncProxyCommandSupport;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.spi.ServiceContext;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class CommandSupportServiceContext implements ServiceContext {
    public static final CommandSupportServiceContext INSTANCE = new CommandSupportServiceContext();
    @Override
    public Optional<Value> getConfig(ConfigKey<String> key) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> getConfig(String key) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> service) {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("all")
    public <T> Optional<T> getService(String service, Class<T> type) {
        if (type == CommandSupport.class) {
            return Optional.of(
                (T)
                    new AsyncProxyCommandSupport(Mono.defer(() -> {
                        return CommandSupportManagerProviders
                            .getCommandSupport(service, Collections.emptyMap());
                    }))
            );
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(String service) {
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> service) {
        return Collections.emptyList();
    }

    @Override
    public <T> List<T> getServices(String service) {
        return Collections.emptyList();
    }
}
