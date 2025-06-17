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
package org.jetlinks.community.plugin.impl.id;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.PluginDataMapping;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.function.Function;

@AllArgsConstructor
public class DefaultPluginDataIdMapper implements PluginDataIdMapper {


    private final ReactiveRepository<PluginDataIdMappingEntity, String> repository;

    private final ConfigStorageManager storageManager;


    @EventListener
    public void handleEvent(EntityCreatedEvent<PluginDataIdMappingEntity> event) {
        event.async(
            saveMapping(Flux.fromIterable(event.getEntity()))
        );
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<PluginDataIdMappingEntity> event) {
        event.async(
            saveMapping(Flux.fromIterable(event.getAfter()))
        );
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<PluginDataIdMappingEntity> event) {
        event.async(
            saveMapping(Flux.fromIterable(event.getEntity()))
        );
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<PluginDataIdMappingEntity> event) {
        event.async(
            removeMapping(Flux.fromIterable(event.getEntity()))
        );
    }

    protected <T> Mono<T> doWithStore(Function<ConfigStorage, Mono<T>> mapper) {
        return storageManager
            .getStorage("plugin-id-mapping")
            .flatMap(mapper);
    }

    private Mono<Void> saveMapping(Flux<PluginDataIdMappingEntity> entityFlux) {
        return this
            .doWithStore(store -> entityFlux
                .flatMap(e -> Flux.just(
                    Tuples.of(
                        createMappingKey(e.getType(), e.getPluginId(), e.getInternalId()),
                        e.getExternalId()
                    ),
                    Tuples.of(
                        createMappingKey(e.getType(), e.getPluginId(), e.getExternalId()),
                        e.getInternalId()
                    )
                ))
                .reduce(new HashMap<String, Object>(), (map, tp2) -> {
                    map.put(tp2.getT1(), tp2.getT2());
                    return map;
                })
                .flatMap(store::setConfigs))
            .then();
    }

    public Mono<Void> removeMapping(Flux<PluginDataIdMappingEntity> entityFlux) {
        return this
            .doWithStore(store -> entityFlux
                .flatMap(e -> Flux.just(
                    createMappingKey(e.getType(), e.getPluginId(), e.getInternalId()),
                    createMappingKey(e.getType(), e.getPluginId(), e.getExternalId())
                ))
                .buffer(200)
                .flatMap(store::remove)
                .then());
    }

    private String createMappingKey(String type, String pluginId, String id) {
        return DigestUtils.md5Hex(String.join("|", type, pluginId, id));
    }

    @Override
    public Mono<String> getInternalId(String type,
                                      String pluginId,
                                      String externalId) {
        Assert.notNull(externalId, "externalId must not be null");
        return doWithStore(store -> store
            .getConfig(createMappingKey(type, pluginId, externalId),
                       Mono.defer(() -> repository
                           .createQuery()
                           .where(PluginDataIdMappingEntity::getType, type)
                           .and(PluginDataIdMappingEntity::getPluginId, pluginId)
                           .and(PluginDataIdMappingEntity::getExternalId, externalId)
                           .fetchOne()
                           .map(PluginDataIdMappingEntity::getInternalId)))
            .map(Value::asString))
            .defaultIfEmpty(externalId);
    }

    @Override
    public Mono<String> getExternalId(String type,
                                      String pluginId,
                                      String internalId) {
        Assert.notNull(internalId, "internalId must not be null");
        return doWithStore(store -> store
            .getConfig(createMappingKey(type, pluginId, internalId),
                       Mono.defer(() -> repository
                           .createQuery()
                           .where(PluginDataIdMappingEntity::getType, type)
                           .and(PluginDataIdMappingEntity::getPluginId, pluginId)
                           .and(PluginDataIdMappingEntity::getInternalId, internalId)
                           .fetchOne()
                           .map(PluginDataIdMappingEntity::getExternalId)))
            .map(Value::asString))
            .defaultIfEmpty(internalId);
    }

    @Override
    public Flux<PluginDataMapping> getMappings(String type, String pluginId) {
        return repository
            .createQuery()
            .where(PluginDataIdMappingEntity::getType, type)
            .and(PluginDataIdMappingEntity::getPluginId, pluginId)
            .fetch()
            .map(entity -> new PluginDataMapping(entity.getExternalId(), entity.getInternalId()));
    }
}
