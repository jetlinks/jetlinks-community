package org.jetlinks.community.network;

import reactor.core.publisher.Mono;

import java.util.List;

public interface NetworkManager {

    <T extends Network> Mono<T> getNetwork(NetworkType type, String id);

    List<NetworkProvider<?>> getProviders();

   Mono<Void> reload(NetworkType type, String id);

   Mono<Void> shutdown(NetworkType type, String id);
}
