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
package org.jetlinks.community.timeseries.query;


import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface AggregationData extends ValueObject {

    Map<String, Object> asMap();

    @Override
    default Optional<Object> get(String name) {
        return Optional.ofNullable(asMap().get(name));
    }

    @Override
    default Map<String, Object> values() {
        return asMap();
    }

    default AggregationData merge(AggregationData another) {
        Map<String, Object> newVal = new HashMap<>(asMap());
        newVal.putAll(another.asMap());
        return of(newVal);
    }

    static AggregationData of(Map<String, Object> map) {
        return () -> map;
    }
}
