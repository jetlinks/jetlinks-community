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

    @Nonnull
    Mono<Void> updateEvent(@Nonnull String thingType,
                           @Nonnull String thingId,
                           @Nonnull String eventId,
                           long timestamp,
                           @Nonnull Object data);

    @Nonnull
    Mono<Void> updateTag(@Nonnull String thingType,
                         @Nonnull String thingId,
                         @Nonnull String tagKey,
                         long timestamp,
                         @Nonnull Object tagValue);

    @Nonnull
    Mono<Void> removeProperty(@Nonnull String thingType,
                              @Nonnull String thingId,
                              @Nonnull String property);

    @Nonnull
    Mono<Void> removeProperties(@Nonnull String thingType,
                                @Nonnull String thingId);

    @Nonnull
    Mono<Void> removeEvent(@Nonnull String thingType,
                           @Nonnull String thingId,
                           @Nonnull String eventId);

    @Nonnull
    Mono<Void> removeTag(@Nonnull String thingType,
                         @Nonnull String thingId,
                         @Nonnull String tagKey);
}
