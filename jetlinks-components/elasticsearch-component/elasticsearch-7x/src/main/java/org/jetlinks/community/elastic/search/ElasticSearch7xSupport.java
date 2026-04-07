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
package org.jetlinks.community.elastic.search;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_index_template.IndexTemplate;
import co.elastic.clients.elasticsearch.indices.get_index_template.IndexTemplateItem;
import co.elastic.clients.elasticsearch.indices.get_index_template.IndexTemplateSummary;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.Version;
import org.jetlinks.community.elastic.search.enums.ElasticSearch7xTermType;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermTypes;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.util.Objects;

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
    public TypeMapping getIndexTemplateMapping(GetIndexTemplateResponse response, String index) {
        for (IndexTemplateItem indexTemplate : response.indexTemplates()) {
            if (Objects.equals(indexTemplate.name(), index)) {
                IndexTemplate template = indexTemplate.indexTemplate();
                IndexTemplateSummary summary = template.template();
                if (summary != null) {
                    return summary.mappings();
                }
            }
        }
        return null;
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
        if (bucket instanceof LongTermsBucket) {
            return CastUtils.castNumber(((LongTermsBucket) bucket).key()).longValue();
        }
        if (bucket instanceof DoubleTermsBucket) {
            return ((DoubleTermsBucket) bucket).key();
        }
        if (bucket instanceof StringTermsBucket) {
            return ((StringTermsBucket) bucket).key()._get();
        }
        if (bucket instanceof DateHistogramBucket _bucket) {
            return _bucket.key();
        }
        if (bucket instanceof HistogramBucket _bucket) {
            return _bucket.key();
        }
        return null;
    }
}