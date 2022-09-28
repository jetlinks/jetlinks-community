package org.jetlinks.community.relation.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.jetlinks.community.relation.entity.RelatedEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 关系配置的数据引用提供商.
 *
 * @author zhangji 2022/6/29
 */
@Component
public class RelationDataReferenceProvider implements DataReferenceProvider {
    private final ReactiveRepository<RelatedEntity, String> reletedRepository;

    public RelationDataReferenceProvider(ReactiveRepository<RelatedEntity, String> reletedRepository) {
        this.reletedRepository = reletedRepository;
    }


    @Override
    public String getId() {
        return DataReferenceManager.TYPE_RELATION;
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String relation) {
        return reletedRepository
            .createQuery()
            .where(RelatedEntity::getRelation, relation)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getId(), getId(), e.getRelation(), e.getRelatedName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return reletedRepository
            .createQuery()
            .where()
            .notNull(RelatedEntity::getRelation)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getId(), getId(), e.getRelation(), e.getRelatedName()));
    }
}
