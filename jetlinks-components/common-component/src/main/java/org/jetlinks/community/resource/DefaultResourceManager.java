package org.jetlinks.community.resource;

import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultResourceManager implements ResourceManager {
    private final Map<String, ResourceProvider> providers = new ConcurrentHashMap<>();

    public DefaultResourceManager() {
    }

    public void addProvider(ResourceProvider provider) {
        providers.put(provider.getType(), provider);
    }

    @Override
    public Flux<Resource> getResources(String type) {
        return getResources(type, null);
    }

    @Override
    public Flux<Resource> getResources(String type, Collection<String> id) {
        return this
            .getProvider(type)
            .flatMapMany(provider -> CollectionUtils.isEmpty(id) ? provider.getResources() : provider.getResources(id));
    }

    private Mono<ResourceProvider> getProvider(String type) {
        return Mono.justOrEmpty(providers.get(type));
    }

}
