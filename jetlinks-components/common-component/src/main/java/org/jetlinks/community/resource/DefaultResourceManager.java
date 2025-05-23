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
