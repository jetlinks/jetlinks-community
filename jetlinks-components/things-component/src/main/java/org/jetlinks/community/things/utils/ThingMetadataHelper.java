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
package org.jetlinks.community.things.utils;

import org.jetlinks.core.things.MetadataId;
import org.jetlinks.core.things.ThingId;
import org.jetlinks.core.things.ThingMetadataType;
import reactor.function.Function3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ThingMetadataHelper<S, T> {

    private final Map<ThingMetadataType, Function3<ThingId, MetadataId, S, T>> mappers = new ConcurrentHashMap<>();

    public static <S, T> ThingMetadataHelper<S, T> create() {
        return new ThingMetadataHelper<>();
    }

    public ThingMetadataHelper<S, T> when(ThingMetadataType type, Function3<ThingId, MetadataId, S, T> mapper) {
        mappers.put(type, mapper);
        return this;
    }

    public ThingMetadataHelper<S, T> when(ThingMetadataType type, BiFunction<MetadataId, S, T> mapper) {
        mappers.put(type, (ignore, mid, t) -> mapper.apply(mid, t));
        return this;
    }

    public Function<S, T> toFunction(MetadataId metadataId) {
        return toFunction(null, metadataId);
    }

    public Function<S, T> toFunction(ThingId thingId, MetadataId metadataId) {
        Function3<ThingId, MetadataId, S, T> function = mappers.get(metadataId.getType());
        if (function == null) {
            throw new UnsupportedOperationException("unsupported metadata type " + metadataId.getType());
        }
        return (source) -> function.apply(thingId, metadataId, source);
    }

}
