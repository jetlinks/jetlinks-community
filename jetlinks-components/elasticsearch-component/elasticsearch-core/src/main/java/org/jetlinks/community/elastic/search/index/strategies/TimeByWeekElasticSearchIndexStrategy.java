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
package org.jetlinks.community.elastic.search.index.strategies;

import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 按每年第n周来划分索引策略
 *
 * @author zhouhao
 * @since 2.3
 */
public class TimeByWeekElasticSearchIndexStrategy extends TemplateElasticSearchIndexStrategy {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private static final WeekFields FIELDS = WeekFields.of(Locale.getDefault());

    public TimeByWeekElasticSearchIndexStrategy(ReactiveElasticsearchClient client,
                                                ElasticSearchIndexProperties properties) {
        super("time-by-week", client, properties);
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now(CLOCK);
        String idx = wrapIndex(index);

        return idx + "_" + now.getYear() + "-woy-" + now.get(FIELDS.weekOfYear());
    }
}
