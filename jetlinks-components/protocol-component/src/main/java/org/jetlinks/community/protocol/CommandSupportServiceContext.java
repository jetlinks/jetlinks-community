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
