package org.jetlinks.community.relation.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.core.things.relation.RelatedObject;
import org.jetlinks.core.things.relation.RelationOperation;
import org.jetlinks.community.relation.entity.RelatedEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
class DefaultRelationOperation implements RelationOperation {
    final String type;
    final String id;
    final ReactiveRepository<RelatedEntity, String> relatedRepository;
    final Function<String, RelationObjectProvider> objectProvider;
    final boolean reverse;

    @Override
    public Flux<RelatedObject> save(String type,
                                    String relation,
                                    Collection<String> targetId) {

        return Flux.empty();
    }

    @Override
    public Mono<RelatedObject> save(String type,
                                    String relation,
                                    String targetId) {
        return Mono.empty();
    }

    @Override
    public Mono<RelatedObject> get(String type,
                                   String relation,
                                   String targetId) {
        return this
            .get(type, relation, Collections.singleton(targetId))
            .singleOrEmpty();
    }

    @Override
    public Flux<RelatedObject> get(String type,
                                   String relation,
                                   String... targetId) {
        return get(type, relation, Arrays.asList(targetId));
    }

    @Override
    public Flux<RelatedObject> get(String type,
                                   String relation,
                                   Collection<String> targetId) {
        return relatedRepository
            .createQuery()
            .where(reverse ? RelatedEntity::getRelatedKey : RelatedEntity::getObjectKey, RelatedEntity.generateKey(this.type, this.id))
            .and(RelatedEntity::getRelation, relation)
            .when(CollectionUtils.isNotEmpty(targetId),
                  query -> query
                      .in(reverse ? RelatedEntity::getObjectKey : RelatedEntity::getRelatedKey,
                          targetId.stream().map(id -> RelatedEntity.generateKey(type, id)).collect(Collectors.toSet())))
            .fetch()
            .map(this::toObject);
    }

    @Override
    public Flux<RelatedObject> get(String type) {
        return get(type, null, Collections.emptyList());
    }

    private RelatedObject toObject(RelatedEntity entity) {
        if (reverse) {
            return new DefaultRelatedObject(
                entity.getObjectType(),
                entity.getObjectId(),
                type,
                id,
                entity.getRelation(),
                relatedRepository,
                objectProvider);
        }
        return new DefaultRelatedObject(
            entity.getRelatedType(),
            entity.getRelatedId(),
            type,
            id,
            entity.getRelation(),
            relatedRepository,
            objectProvider);
    }

}
