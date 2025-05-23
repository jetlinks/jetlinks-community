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
package org.jetlinks.community.plugin.context;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.plugin.core.ServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class CompositeServiceRegistry implements ServiceRegistry {
    private final List<ServiceRegistry> registries;

    @Override
    public <T> Optional<T> getService(Class<T> type) {
        for (ServiceRegistry registry : registries) {
            Optional<T> service = registry.getService(type);
            if (service.isPresent()) {
                return service;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> type, String name) {
        for (ServiceRegistry registry : registries) {
            Optional<T> service = registry.getService(type, name);
            if (service.isPresent()) {
                return service;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> type) {
        for (ServiceRegistry registry : registries) {
            List<T> service = registry.getServices(type);
            if (CollectionUtils.isNotEmpty(service)) {
                return service;
            }
        }
        return Collections.emptyList();
    }
}
