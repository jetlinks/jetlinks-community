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
package org.jetlinks.community.config;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.cache.ReactiveCache;
import org.hswebframework.web.cache.ReactiveCacheManager;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.config.entity.ConfigEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class SimpleConfigManager implements ConfigManager, ConfigScopeManager {

    private final Map<ConfigScope, Set<ConfigPropertyDef>> scopes = new ConcurrentHashMap<>();

    private final ReactiveRepository<ConfigEntity, String> repository;

    private final ReactiveCache<Map<String, Object>> cache;

    public SimpleConfigManager(ReactiveRepository<ConfigEntity, String> repository, ReactiveCacheManager cacheManager) {
        this.repository = repository;
        this.cache = cacheManager.getCache("system-config");
    }

    @Override
    public void addScope(ConfigScope scope,
                         List<ConfigPropertyDef> properties) {
        scopes.computeIfAbsent(scope, ignore -> new LinkedHashSet<>())
              .addAll(properties);
    }

    @Override
    public Flux<ConfigScope> getScopes() {
        return Flux.fromIterable(scopes.keySet());
    }

    @Override
    public Mono<ConfigScope> getScope(String scope) {
        return this
            .getScopes()
            .filter(configScope -> Objects.equals(configScope.getId(), scope))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public Flux<ConfigPropertyDef> getPropertyDef(String scope) {
        return Flux.fromIterable(scopes.getOrDefault(
            ConfigScope.of(scope, scope, false),
            Collections.emptySet()));
    }

    @Override
    public Mono<ValueObject> getProperties(String scope) {
        return Mono
            .zip(
                //默认值
                getPropertyDef(scope)
                    .filter(def -> null != def.getDefaultValue())
                    .collectMap(ConfigPropertyDef::getKey, ConfigPropertyDef::getDefaultValue),
                //数据库配置的值
                cache
                    .getMono(scope, () -> getPropertiesNow(scope)),
                (defaults, values) -> {
                    Map<String, Object> properties = new HashMap<>(values);
                    defaults.forEach(properties::putIfAbsent);
                    return properties;
                }
            )
            .map(ValueObject::of);
    }

    private Mono<Map<String, Object>> getPropertiesNow(String scope) {
        return repository
            .createQuery()
            .where(ConfigEntity::getScope, scope)
            .fetch()
            .filter(val -> MapUtils.isNotEmpty(val.getProperties()))
            .reduce(new LinkedHashMap<>(), (l, r) -> {
                l.putAll(r.getProperties());
                return l;
            });
    }

    @Override
    public Mono<Void> setProperties(String scope, Map<String, Object> values) {
        ConfigEntity entity = new ConfigEntity();
        entity.setProperties(values);
        entity.setScope(scope);
        entity.getId();
        return repository
            .save(entity)
            .then(cache.evict(scope));
    }

}
