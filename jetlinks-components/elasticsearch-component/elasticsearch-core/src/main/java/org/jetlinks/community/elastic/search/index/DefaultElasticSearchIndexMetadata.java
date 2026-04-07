/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.elastic.search.index;

import lombok.Generated;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;

import java.util.*;

public class DefaultElasticSearchIndexMetadata implements ElasticSearchIndexMetadata {
    private final String index;

    private final Map<String, PropertyMetadata> properties = new LinkedHashMap<>();

    private final String timestampProperty;

    private final Map<String, Object> settings = new HashMap<>();

    public DefaultElasticSearchIndexMetadata(String index) {
        this(index, "timestamp");
    }

    public DefaultElasticSearchIndexMetadata(String index, String timestampProperty) {
        this.index = index.toLowerCase().trim();
        this.timestampProperty = timestampProperty;
    }

    public DefaultElasticSearchIndexMetadata(String index, List<PropertyMetadata> properties) {
        this(index);
        properties.forEach(this::addProperty);
    }

    public DefaultElasticSearchIndexMetadata(String index, List<PropertyMetadata> properties, String timestampProperty) {
        this(index, timestampProperty);
        properties.forEach(this::addProperty);
    }

    @Override
    public Map<String, Object> getSettings() {
        return settings;
    }

    public DefaultElasticSearchIndexMetadata withSetting(String key, Object value){
        settings.put(key,value);
        return this;
    }


    @Override
    public PropertyMetadata getTimestampProperty() {
        return getProperty(timestampProperty);
    }

    @Override
    @Generated
    public PropertyMetadata getProperty(String property) {
        return properties.get(property);
    }

    @Override
    @Generated
    public String getIndex() {
        return index;
    }

    @Override
    @Generated
    public List<PropertyMetadata> getProperties() {
        return new ArrayList<>(properties.values());
    }

    public DefaultElasticSearchIndexMetadata addProperty(PropertyMetadata property) {
        properties.put(property.getId(), property);
        return this;
    }

    public DefaultElasticSearchIndexMetadata addProperty(String property, DataType type) {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setValueType(type);
        metadata.setId(property);
        addProperty(metadata);
        return this;
    }

    @Override
    public ElasticSearchIndexMetadata newIndexName(String name) {
        DefaultElasticSearchIndexMetadata metadata = new DefaultElasticSearchIndexMetadata(
            name,new ArrayList<>(properties.values()),timestampProperty
        );
        metadata.settings.putAll(settings);

        return metadata;
    }
}
