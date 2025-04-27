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
