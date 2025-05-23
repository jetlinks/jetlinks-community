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
