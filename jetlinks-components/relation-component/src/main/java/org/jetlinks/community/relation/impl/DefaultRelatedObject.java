package org.jetlinks.community.relation.impl;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.core.things.relation.RelatedObject;
import org.jetlinks.community.relation.entity.RelatedEntity;

import java.util.function.Function;

class DefaultRelatedObject extends DefaultRelationObject implements RelatedObject {

    private final String relatedType;
    private final String relatedId;
    private final String relation;

    public DefaultRelatedObject(String type,
                                String id,
                                String relatedType,
                                String relatedId,
                                String relation,
                                ReactiveRepository<RelatedEntity, String> relatedRepository,
                                Function<String, RelationObjectProvider> objectProvider) {
        super(type, id, relatedRepository, objectProvider);
        this.relatedType = relatedType;
        this.relatedId = relatedId;
        this.relation = relation;
    }

    @Override
    public String getRelation() {
        return relation;
    }

    @Override
    public String getRelatedToId() {
        return relatedId;
    }

    @Override
    public String getRelatedToType() {
        return relatedType;
    }


}
