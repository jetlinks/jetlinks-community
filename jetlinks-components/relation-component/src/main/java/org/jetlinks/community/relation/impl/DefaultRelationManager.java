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
package org.jetlinks.community.relation.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.entity.RelatedEntity;
import org.jetlinks.community.relation.entity.RelationEntity;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.things.relation.RelationManager;
import org.jetlinks.core.things.relation.RelationObject;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DefaultRelationManager implements RelationManager {

    private final ReactiveRepository<RelatedEntity, String> relatedRepository;

    private final ReactiveRepository<RelationEntity, String> relationRepository;

    protected final Map<String, RelationObjectProvider> providers = new ConcurrentHashMap<>();

    public void addProvider(RelationObjectProvider provider) {
        providers.put(provider.getTypeId(), provider);
    }

    @Override
    public Mono<ObjectType> getObjectType(String typeId) {
        RelationObjectProvider provider = getProvider(typeId);

        return provider
            .getType()
            .flatMap(type -> relationRepository
                .createQuery()
                //动态关系双向查询
                .where(RelationEntity::getObjectType, typeId)
                .or(RelationEntity::getTargetType, typeId)
                .fetch()
                .collect(Collectors.groupingBy(
                    //根据关系对象分组
                    entity -> typeId.equals(entity.getObjectType()) ? entity.getTargetType() : entity.getObjectType(),
                    Collectors.mapping(e -> SimpleRelation.of(typeId, e), Collectors.toList())))
                .flatMap(group -> fillRelations(type, group)));
    }

    private Mono<ObjectType> fillRelations(ObjectType type, Map<String, List<SimpleRelation>> relations) {
        String typeId = type.getId();
        SimpleObjectType custom = new SimpleObjectType(typeId, type.getName(), type.getDescription());
        for (Map.Entry<String, List<SimpleRelation>> entry : relations.entrySet()) {
            //添加动态关系
            custom.withRelation(entry.getKey(), entry.getValue());
        }
        return getObjectTypes()
            .doOnNext(other -> {
                if (!typeId.equals(other.getId())) {
                    List<Relation> fixRelations = other.getRelations(typeId);
                    if (CollectionUtils.isNotEmpty(other.getRelations(typeId))) {
                        //添加其它类型提供的固定关系
                        custom.withRelation(other.getId(), fixRelations
                            .stream()
                            .map(r -> SimpleRelation.of(r.getId(), r.getName(), r.getReverseName(), !r.isReverse(), r.getExpands()))
                            .collect(Collectors.toList()));
                    }
                }
            })
            .then(Mono.just(new CompositeObjectType(type, custom)));
    }

    @Override
    public Flux<ObjectType> getObjectTypes() {
        return Flux
            .fromIterable(providers.values())
            .flatMap(RelationObjectProvider::getType);
    }

    @Override
    public Mono<RelationObject> getObject(String objectType,
                                          String objectId) {
        if (!StringUtils.hasText(objectType) || !StringUtils.hasText(objectId)) {
            return Mono.empty();
        }
        return Mono
            .just(
                new DefaultRelationObject(
                    objectType,
                    objectId,
                    relatedRepository,
                    this::getProvider)
            );
    }

    @Override
    public Flux<RelationObject> getObjects(String objectType, Collection<String> objectIds) {
        return Flux.fromIterable(objectIds)
                   .flatMap(id -> getObject(objectType, id));
    }

    private RelationObjectProvider getProvider(String type) {
        RelationObjectProvider provider = providers.get(type);
        if (provider == null) {
            throw new I18nSupportException("error.unsupported_object_type", type);
        }
        return provider;
    }
}
