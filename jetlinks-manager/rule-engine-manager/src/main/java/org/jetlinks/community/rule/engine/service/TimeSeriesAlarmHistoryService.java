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
package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouhao
 * @since 2.2
 */
@AllArgsConstructor
public class TimeSeriesAlarmHistoryService implements AlarmHistoryService {

    //兼容es索引
    public final static String ALARM_HISTORY_METRIC = "alarm_history";

    private final TimeSeriesManager manager;

    @Override
    public Flux<AggregationData> aggregation(AggregationQueryParam param) {
        return manager
            .getService(ALARM_HISTORY_METRIC)
            .aggregation(param);
    }

    @Override
    public Flux<AlarmHistoryInfo> query(QueryParam param) {
        return manager
            .getService(ALARM_HISTORY_METRIC)
            .query(param)
            .map(ts -> ts.as(AlarmHistoryInfo.class));
    }

    @Override
    public Mono<Long> count(QueryParam queryParam) {
        return manager
            .getService(ALARM_HISTORY_METRIC)
            .count(queryParam)
            .map(Number::longValue);
    }

    public Mono<PagerResult<AlarmHistoryInfo>> queryPager(QueryParam queryParam) {
        return manager
            .getService(ALARM_HISTORY_METRIC)
            .queryPager(queryParam, ts -> ts.as(AlarmHistoryInfo.class));
    }

    public Mono<Void> save(AlarmHistoryInfo historyInfo) {
        return manager
            .getService(ALARM_HISTORY_METRIC)
            .commit(createData(historyInfo));
    }

    public Mono<Void> save(Flux<AlarmHistoryInfo> historyInfo) {
        return manager
            .getService(ALARM_HISTORY_METRIC)
            .save(historyInfo.map(this::createData));
    }

    public Mono<Void> save(Mono<AlarmHistoryInfo> historyInfo) {
        return save(historyInfo.flux());
    }

    private TimeSeriesData createData(AlarmHistoryInfo info) {
        Map<String, Object> data = FastBeanCopier.copy(info, new HashMap<>(16));

        return TimeSeriesData.of(info.getAlarmTime(), data);

    }

    @PostConstruct
    public void init() {
        // 大字符,在某些关系型数据库的实现上,使用longvarchar存储.
        StringType longStringType = new StringType()
            .expand(ConfigMetadataConstants.maxLength, 8000L);

        manager.registerMetadata(
            TimeSeriesMetadata.of(
                TimeSeriesMetric.of(ALARM_HISTORY_METRIC),
                SimplePropertyMetadata.of("id", "ID", StringType.GLOBAL),

                SimplePropertyMetadata.of("alarmConfigId", "告警配置ID", StringType.GLOBAL),
                SimplePropertyMetadata.of("alarmConfigName", "告警配置名称", StringType.GLOBAL),
                SimplePropertyMetadata.of("alarmConfigSource", "告警配置来源", StringType.GLOBAL),

                SimplePropertyMetadata.of("alarmRecordId", "告警记录ID", StringType.GLOBAL),

                SimplePropertyMetadata.of("level", "告警级别", IntType.GLOBAL),
                SimplePropertyMetadata.of("description", "说明", StringType.GLOBAL),
                SimplePropertyMetadata.of("alarmTime", "告警时间", DateTimeType.GLOBAL),

                SimplePropertyMetadata.of("targetType", "告警目标类型", StringType.GLOBAL),
                SimplePropertyMetadata.of("targetName", "告警目标名称", StringType.GLOBAL),
                SimplePropertyMetadata.of("targetId", "告警目标ID", StringType.GLOBAL),

                SimplePropertyMetadata.of("sourceType", "告警来源类型", StringType.GLOBAL),
                SimplePropertyMetadata.of("sourceName", "告警来源名称", StringType.GLOBAL),
                SimplePropertyMetadata.of("sourceId", "告警来源ID", StringType.GLOBAL),

                SimplePropertyMetadata.of("alarmInfo", "告警信息", longStringType),
                SimplePropertyMetadata.of("creatorId", "创建人ID", StringType.GLOBAL),
                SimplePropertyMetadata.of("termSpec", "告警条件描述", longStringType),
                SimplePropertyMetadata.of("triggerDesc", "告警触发描述", longStringType),
                SimplePropertyMetadata.of("actualDesc", "实际触发描述", longStringType),
                SimplePropertyMetadata.of("bindings", "绑定信息", new ArrayType().elementType(StringType.GLOBAL))


            )).block(Duration.ofSeconds(10));
    }
}
