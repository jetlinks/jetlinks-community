package org.jetlinks.community.things.data;

import org.jetlinks.core.things.ThingProperty;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public interface ThingsDataWriter {

    @Nonnull
    Mono<Void> updateProperty(@Nonnull String thingType,
                              @Nonnull String thingId,
                              @Nonnull ThingProperty property);

    @Nonnull
    Mono<Void> updateProperty(@Nonnull String thingType,
                              @Nonnull String thingId,
                              @Nonnull String property,
                              long timestamp,
                              @Nonnull Object value,
                              String state);
}
