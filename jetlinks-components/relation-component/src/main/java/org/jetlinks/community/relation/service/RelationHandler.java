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
