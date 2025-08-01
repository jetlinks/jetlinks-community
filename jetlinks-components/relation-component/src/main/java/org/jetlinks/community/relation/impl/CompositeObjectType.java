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
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.CompositeMap;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.Relation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class CompositeObjectType implements ObjectType {
    private final ObjectType main;
    private final ObjectType second;

    @Override
    public List<PropertyMetadata> getProperties() {
        return Stream
            .concat(main.getProperties().stream(), second.getProperties().stream())
            .collect(Collectors.toList());
    }

    @Override
    public List<Relation> getRelations(String type) {
        return Stream
            .concat(main.getRelations(type).stream(), second.getRelations(type).stream())
            .collect(Collectors.toList());
    }

    @Override
    public List<ObjectType> getRelatedTypes() {
        return Stream
            .concat(main.getRelatedTypes().stream(), second.getRelatedTypes().stream())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Relation>> getRelations() {
        return new CompositeMap<>(main.getRelations(), second.getRelations());
    }

    @Override
    public String getId() {
        return main.getId();
    }

    @Override
    public String getName() {
        return main.getName();
    }

    @Override
    public String getDescription() {
        return main.getDescription();
    }

    @Override
    public Map<String, Object> getExpands() {
        Map<String, Object> expands = new HashMap<>();
        if (MapUtils.isNotEmpty(main.getExpands())) {
            expands.putAll(main.getExpands());
        }
        if (MapUtils.isNotEmpty(second.getExpands())) {
            expands.putAll(second.getExpands());
        }
        return expands;
    }
}
