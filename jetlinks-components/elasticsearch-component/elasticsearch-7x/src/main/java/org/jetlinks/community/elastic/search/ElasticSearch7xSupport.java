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
package org.jetlinks.community.elastic.search;

import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.Version;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermTypes;
import org.jetlinks.community.elastic.search.enums.ElasticSearch7xTermType;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;

public class ElasticSearch7xSupport extends ElasticSearchSupport {

    static {
        Version version = Version.VERSION;
        if (version != null && version.major() == 7) {
            for (ElasticSearch7xTermType value : ElasticSearch7xTermType.values()) {
                ElasticSearchTermTypes.register(value);
            }
        }
    }

    @Override
    public IndexSettings.Builder applyIndexSettings(ElasticSearchIndexProperties index, IndexSettings.Builder builder) {
        return super
            .applyIndexSettings(index, builder)
            .mapping(b -> b
                .totalFields(t -> t.limit((int) index.getTotalFieldsLimit())));
    }

    @Override
    public DynamicTemplate createDynamicTemplate(String type, Property property) {
        return DynamicTemplate
            .of(b -> b
                .matchMappingType(type)
                .mapping(property));
    }

    @Override
    public TemplateMapping getTemplateMapping(GetTemplateResponse response, String index) {
        return response.get(index);
    }

    @Override
    public IndexState getIndexState(GetIndexResponse response, String index) {
        return response.get(index);
    }

    @Override
    public IndexMappingRecord getIndexMapping(GetMappingResponse response, String index) {
        return response.get(index);
    }

    @Override
    public Object getBucketKey(MultiBucketBase bucket) {
        if (bucket instanceof DateHistogramBucket _bucket) {
            return _bucket.key();
        }
        if (bucket instanceof HistogramBucket _bucket) {
            return _bucket.key();
        }
        return null;
    }
}
