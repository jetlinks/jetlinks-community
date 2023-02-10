package org.jetlinks.community.relation.service;

import org.hswebframework.web.crud.events.EntityBeforeDeleteEvent;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.relation.entity.RelationEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 输入描述.
 *
 * @author zhangji 2022/6/29
 */
@Component
public class RelationHandler {
    private final DataReferenceManager referenceManager;

    public RelationHandler(DataReferenceManager referenceManager) {
        this.referenceManager = referenceManager;
    }

    //禁止删除存在关系的关系配置
    @EventListener
    public void handleRelationDeleted(EntityBeforeDeleteEvent<RelationEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(relation -> referenceManager
                    .assertNotReferenced(DataReferenceManager.TYPE_RELATION, relation.getRelation()))
        );
    }
}
