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
package org.jetlinks.community.relation.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.relation.entity.RelatedEntity;
import org.jetlinks.community.relation.entity.RelationEntity;
import org.jetlinks.community.relation.service.request.SaveRelationRequest;
import org.jetlinks.community.relation.service.response.RelatedInfo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.function.Function;

@Service
@AllArgsConstructor
public class RelationService extends GenericReactiveCrudService<RelationEntity, String> {

    private final ReactiveRepository<RelatedEntity, String> relatedRepository;

    public Flux<RelatedInfo> getRelationInfo(String type, Collection<String> idList) {
        return Mono
            .zip(
                //关系定义
                this
                    .createQuery()
                    .where(RelationEntity::getObjectType, type)
                    .orderBy(SortOrder.asc(RelationEntity::getCreateTime))
                    .fetch()
                    .collectList()
                    .filter(CollectionUtils::isNotEmpty),
                //已建立的关系信息
                relatedRepository
                    .createQuery()
                    .in(RelatedEntity::getObjectKey, RelatedEntity.generateKey(type, idList))
                    .fetch()
                    //按关系对象分组
                    .groupBy(RelatedEntity::getObjectId)
                    // <关系对象,<关系,List<关系对象>>>
                    .flatMap(group -> group
                        //按关系分组
                        .groupBy(rel -> Tuples.of(rel.getRelatedType(), rel.getRelation()))
                        //<关系,List<关系对象>>
                        .flatMap(relateGroup -> relateGroup
                            .map(RelatedObjectInfo::ofRelated)
                            .collectList()
                            .map(list -> Tuples.of(relateGroup.key(), list)))
                        .collectMap(Tuple2::getT1, Tuple2::getT2)
                        .map(mapping -> Tuples.of(group.key(), mapping)))
                    .collectMap(Tuple2::getT1, Tuple2::getT2),
                (relations, relatedMapping) -> Flux
                    .fromIterable(idList)
                    .flatMap(objectId -> Flux
                        .fromIterable(relations)
                        .map(relation -> {
                            RelatedInfo relatedInfo = new RelatedInfo();
                            relatedInfo.setObjectId(objectId);
                            relatedInfo.setRelation(relation.getRelation());
                            relatedInfo.setRelationName(relation.getName());
                            relatedInfo.setRelatedType(relatedInfo.getRelatedType());
                            relatedInfo.setRelationExpands(relatedInfo.getRelationExpands());
                            List<RelatedObjectInfo> related = relatedMapping
                                .getOrDefault(objectId, Collections.emptyMap())
                                .get(Tuples.of(relation.getTargetType(), relation.getRelation()));
                            relatedInfo.setRelated(related);
                            return relatedInfo;
                        }))
            )
            .flatMapMany(Function.identity());

    }

    public Flux<RelatedInfo> getRelationInfo(String type, String id) {
        return getRelationInfo(type, Collections.singletonList(id));
    }

    public Mono<Void> saveRelated(String type, String id, Flux<SaveRelationRequest> requestFlux) {

        return requestFlux
            .groupBy(request -> Tuples.of(request.getRelatedType(), request.getRelation()))
            .flatMap(group -> relatedRepository
                .createDelete()
                .where(RelatedEntity::getObjectKey, RelatedEntity.generateKey(type, id))
                .and(RelatedEntity::getRelatedType, group.key().getT1())
                .and(RelatedEntity::getRelation, group.key().getT2())
                .execute()
                .thenMany(group))
            .filter(request -> CollectionUtils.isNotEmpty(request.getRelated()))
            .flatMap(request -> Flux
                .fromIterable(request.getRelated())
                .map(related -> new RelatedEntity()
                    .withObject(type, id)
                    .withRelated(request.getRelatedType(), related, request.getRelation())))
            .as(relatedRepository::insert)
            .then();
    }

}
