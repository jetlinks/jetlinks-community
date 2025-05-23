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
package org.jetlinks.community.datasource.command;

import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.community.datasource.DataSourceProvider;
import org.jetlinks.community.spi.Provider;
import reactor.core.publisher.Mono;

public interface CommandHandlerProvider {

    Provider<CommandHandlerProvider> supports = Provider.create(CommandHandlerProvider.class);

    String getType();

    Mono<CommandHandler<?, ?>> createCommandHandler(DataSourceProvider.CommandConfiguration configuration);

}
