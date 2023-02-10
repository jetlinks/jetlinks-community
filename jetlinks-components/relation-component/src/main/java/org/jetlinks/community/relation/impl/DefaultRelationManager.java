package org.jetlinks.community.relation.impl;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.core.things.relation.*;
import org.jetlinks.community.relation.entity.RelatedEntity;
import org.jetlinks.community.relation.entity.RelationEntity;
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
                .where(RelationEntity::getObjectType, typeId)
                .fetch()
                .collect(Collectors.groupingBy(
                    RelationEntity::getTargetType,
                    Collectors.mapping(SimpleRelation::of, Collectors.toList())))
                .<ObjectType>map(group -> {
                    SimpleObjectType custom = new SimpleObjectType(typeId, type.getName(), type.getDescription());
                    for (Map.Entry<String, List<SimpleRelation>> entry : group.entrySet()) {
                        custom.withRelation(entry.getKey(), entry.getValue());
                    }
                    return new CompositeObjectType(type, custom);
                })
                .defaultIfEmpty(type));
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
