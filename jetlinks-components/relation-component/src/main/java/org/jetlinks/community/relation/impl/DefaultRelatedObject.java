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
