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
