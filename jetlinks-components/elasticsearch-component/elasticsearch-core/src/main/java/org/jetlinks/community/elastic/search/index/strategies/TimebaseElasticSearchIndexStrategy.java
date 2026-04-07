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
package org.jetlinks.community.elastic.search.index.strategies;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.Interval;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.jetlinks.community.elastic.search.utils.QueryParamTranslator;
import org.jetlinks.reactor.ql.utils.CastUtils;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TimebaseElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {
    public TimebaseElasticSearchIndexStrategy(String id, ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super(id, client, properties);
    }

    protected abstract String getIndexForSave(String index, LocalDateTime time);

    protected abstract Interval getInterval();

    @Override
    public String getIndexForSave(ElasticSearchIndexMetadata index, Map<String, Object> data) {
        PropertyMetadata prop = index.getTimestampProperty();
        if (prop == null) {
            return getIndexForSave(index.getIndex());
        }
        Number tsMaybe = CastUtils.castNumber(data.get(prop.getId()), ignore -> null);
        if (tsMaybe == null) {
            return super.getIndexForSave(index, data);
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(tsMaybe.longValue()),
            ZoneId.systemDefault());

        return getIndexForSave(index.getIndex(), dateTime);
    }

    @Override
    public List<String> getIndexForSearch(ElasticSearchIndexMetadata index, @Nullable QueryParam param) {
        PropertyMetadata prop = index.getTimestampProperty();
        if (prop == null || param == null) {
            return super.getIndexForSearch(index, param);
        }
        List<Long> range = QueryParamTranslator
            .resolveQueryTimestampRange(prop.getId(), param.getTerms());
        if (CollectionUtils.isEmpty(range) || range.size() < 2) {
            return super.getIndexForSearch(index, param);
        }
        long startWith = range.get(0);
        long endWith = range.get(1);
        String baseIndex = index.getIndex();
        List<String> list = new ArrayList<>();
        for (long time : getInterval().iterate(startWith, endWith)) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
            list.add(getIndexForSave(baseIndex, dateTime));
        }
        return list;
    }
}
