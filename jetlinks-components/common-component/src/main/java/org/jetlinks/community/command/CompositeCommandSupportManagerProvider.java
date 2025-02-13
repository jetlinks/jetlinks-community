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
