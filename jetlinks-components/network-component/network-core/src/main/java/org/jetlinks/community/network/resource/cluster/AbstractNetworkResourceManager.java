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
package org.jetlinks.community.network.resource.cluster;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.community.network.resource.NetworkResource;
import org.jetlinks.community.network.resource.NetworkResourceManager;
import org.jetlinks.community.network.resource.NetworkResourceUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractNetworkResourceManager implements NetworkResourceManager {

    private final List<NetworkResourceUser> resourceUsers;

    public AbstractNetworkResourceManager(List<NetworkResourceUser> resourceUser) {
        this.resourceUsers = resourceUser;
    }

    @Override
    public final Flux<NetworkResource> getAliveResources() {

        Mono<Map<String, List<NetworkResource>>> usedMapping = this
            .getLocalUsedResources()
            .collect(Collectors.groupingBy(NetworkResource::getHost))
            .cache();
        return this
            .getLocalAllResources()
            .map(NetworkResource::copy)
            .flatMap(resource -> usedMapping
                .map(usedMap -> {
                    List<NetworkResource> usedList = usedMap.get(resource.getHost());
                    if (CollectionUtils.isNotEmpty(usedList)) {
                        for (NetworkResource used : usedList) {
                            resource.removePorts(used.getPorts());
                        }
                    }
                    return resource;
                }));
    }

    public final Flux<NetworkResource> getLocalUsedResources() {
        return Flux
            .fromIterable(resourceUsers)
            .flatMap(NetworkResourceUser::getUsedResources);
    }

    public abstract Flux<NetworkResource> getLocalAllResources();

}
