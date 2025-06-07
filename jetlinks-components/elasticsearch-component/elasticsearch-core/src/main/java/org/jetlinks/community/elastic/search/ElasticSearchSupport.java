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
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.Version;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

@Slf4j
public abstract class ElasticSearchSupport {

    private static ElasticSearchSupport support;

    static {
        ServiceLoader
            .load(ElasticSearchSupport.class)
            .forEach(ElasticSearchSupport::register);
    }

    public static void load() {

    }

    public int majorVersion() {
        return Version.VERSION == null ? 8 : Version.VERSION.major();
    }

    public boolean is8x() {
        return majorVersion() == 8;
    }

    public boolean is7x() {
        return majorVersion() == 7;
    }

    public static ElasticSearchSupport current() {
        if (support == null) {
            throw new UnsupportedOperationException("当前环境不支持elasticsearch,请添加依赖`elasticsearch-8x`.");
        }
        return support;
    }

    private static void register(ElasticSearchSupport support) {
        if (ElasticSearchSupport.support != null) {
            log.warn("ignore register elasticsearch support:{}", support.getClass());
            return;
        }
        support.setup();

        ElasticSearchSupport.support = support;

        log.info("register elasticsearch support:{}", support.getClass());
    }


    protected void setup() {

    }

    public IndexSettings.Builder applyIndexSettings(ElasticSearchIndexProperties index,
                                                    IndexSettings.Builder builder) {

        builder.numberOfShards(String.valueOf(Math.max(1, index.getNumberOfShards())))
               .numberOfReplicas(String.valueOf(index.getNumberOfReplicas()));

        if (MapUtils.isNotEmpty(index.getOptions())) {
            builder.otherSettings(Maps.transformValues(index.getOptions(), JsonData::of));
        }
        return builder;
    }

    public abstract DynamicTemplate createDynamicTemplate(String type, Property property);

    public abstract IndexState getIndexState(GetIndexResponse response, String index);

    public abstract TemplateMapping getTemplateMapping(GetTemplateResponse response, String index);

    public abstract IndexMappingRecord getIndexMapping(GetMappingResponse response, String index);


    public abstract Object getBucketKey(MultiBucketBase bucket);

    public Map<String, Object> transformBucket(String name,
                                               Map<String, Object> map,
                                               MultiBucketBase bucket) {
        if (bucket instanceof DateHistogramBucket _bucket) {
            Map<String, Object> val = new HashMap<>(map);
            val.put(name, _bucket.keyAsString());
            val.put("_" + name, getBucketKey(_bucket));
            return val;
        } else if (bucket instanceof HistogramBucket _bucket) {
            Map<String, Object> val = new HashMap<>(map);
            val.put(name, _bucket.keyAsString());
            val.put("_" + name, getBucketKey(_bucket));
            return val;
        }
        return map;
    }
}
