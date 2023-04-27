package org.jetlinks.community.network.resource.cluster;

import org.jetlinks.community.network.resource.NetworkResource;
import org.jetlinks.community.network.resource.NetworkResourceUser;
import reactor.core.publisher.Flux;

import java.util.List;

public class DefaultNetworkResourceManager extends AbstractNetworkResourceManager {

    private final NetworkResourceProperties properties;

    public DefaultNetworkResourceManager( NetworkResourceProperties properties,
                                         List<NetworkResourceUser> resourceUser) {
        super(resourceUser);
        this.properties = properties;
    }

    @Override
    public Flux<NetworkResource> getLocalAllResources() {
        return Flux.fromIterable(properties.parseResources());
    }
}
