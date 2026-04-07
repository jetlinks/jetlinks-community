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

import org.jetlinks.community.Interval;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
/**
 * 按月对来划分索引策略
 *
 * @author zhouhao
 * @since 1.0
 */
public class TimeByMonthElasticSearchIndexStrategy extends TimebaseElasticSearchIndexStrategy {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    static final Interval INTERVAL = Interval.ofMonth(1);

    public TimeByMonthElasticSearchIndexStrategy(ReactiveElasticsearchClient client, ElasticSearchIndexProperties properties) {
        super("time-by-month", client, properties);
    }

    @Override
    protected List<String> getIndexPatterns(String index) {
        return Collections.singletonList(wrapIndex(index).concat("_*-*"));
    }

    @Override
    public String getIndexForSave(String index) {
        LocalDate now = LocalDate.now(CLOCK);
        String idx = wrapIndex(index);
        return idx + "_" + now.getYear() + "-" + now.getMonthValue();
    }


    @Override
    protected String getIndexForSave(String index, LocalDateTime time) {
        String idx = wrapIndex(index);
        return idx + "_" + time.getYear() + "-" + time.getMonthValue();
    }

    @Override
    protected Interval getInterval() {
        return INTERVAL;
    }
}
