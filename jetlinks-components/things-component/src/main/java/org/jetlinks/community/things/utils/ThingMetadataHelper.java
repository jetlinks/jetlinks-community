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
