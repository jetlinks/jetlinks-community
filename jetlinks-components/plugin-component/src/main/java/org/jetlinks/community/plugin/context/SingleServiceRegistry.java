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
import org.jetlinks.plugin.core.ServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
public class SingleServiceRegistry implements ServiceRegistry {

    private final String name;
    private final Object service;

    @Override
    public <T> Optional<T> getService(Class<T> type) {

        if (type.isInstance(service)) {
            return Optional.of(type.cast(service));
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> type, String name) {
        if (Objects.equals(name, this.name)) {
            return getService(type);
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> type) {
        if (type.isInstance(service)) {
            return Collections.singletonList(type.cast(service));
        }
        return Collections.emptyList();
    }
}
