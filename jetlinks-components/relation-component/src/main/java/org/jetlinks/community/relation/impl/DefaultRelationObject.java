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
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.core.things.relation.RelationObject;
import org.jetlinks.core.things.relation.RelationOperation;
import org.jetlinks.community.relation.entity.RelatedEntity;

import java.util.function.Function;

@AllArgsConstructor
class DefaultRelationObject implements RelationObject {
    private final String type;
    private final String id;
    private final ReactiveRepository<RelatedEntity, String> relatedRepository;
    private final Function<String, RelationObjectProvider> objectProvider;

    public final RelationObjectProvider getProvider() {
        return objectProvider.apply(type);
    }

    @Override
    public final RelationOperation relations(boolean reverse) {
        return new DefaultRelationOperation(type, id, relatedRepository, objectProvider, reverse);
    }

    @Override
    public final PropertyOperation properties() {
        return getProvider().properties(id);
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getType() {
        return type;
    }

}
