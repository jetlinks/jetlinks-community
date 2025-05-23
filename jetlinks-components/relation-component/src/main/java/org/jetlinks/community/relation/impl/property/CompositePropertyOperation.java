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
package org.jetlinks.community.relation.impl.property;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
class CompositePropertyOperation implements PropertyOperationStrategy {

    private final List<PropertyOperationStrategy> strategies;

    @Override
    public boolean isSupported(String key) {
        return getStrategy(key) != null;
    }

    private PropertyOperationStrategy getStrategy(String key) {
        for (PropertyOperationStrategy strategy : strategies) {
            if (strategy.isSupported(key)) {
                return strategy;
            }
        }
        return null;
    }

    @Override
    public Mono<Object> get(String key) {
        PropertyOperationStrategy strategy = getStrategy(key);
        if (strategy == null) {
            return Mono.empty();
        }
        return strategy.get(key);
    }


}
