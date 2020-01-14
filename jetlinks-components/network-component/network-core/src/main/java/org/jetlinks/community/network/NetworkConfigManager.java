package org.jetlinks.community.network;

import reactor.core.publisher.Mono;

public interface NetworkConfigManager {

    Mono<NetworkProperties> getConfig(NetworkType networkType, String id);

}
