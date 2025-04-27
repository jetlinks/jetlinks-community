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

                SimplePropertyMetadata.of("alarmConfigId", "ID", StringType.GLOBAL),
                SimplePropertyMetadata.of("alarmConfigName", "ID", StringType.GLOBAL),
                SimplePropertyMetadata.of("alarmRecordId", "ID", StringType.GLOBAL),

                SimplePropertyMetadata.of("level", "level", IntType.GLOBAL),
                SimplePropertyMetadata.of("description", "description", StringType.GLOBAL),
                SimplePropertyMetadata.of("alarmTime", "alarmTime", DateTimeType.GLOBAL),

                SimplePropertyMetadata.of("targetType", "targetType", StringType.GLOBAL),
                SimplePropertyMetadata.of("targetName", "targetName", StringType.GLOBAL),
                SimplePropertyMetadata.of("targetId", "targetId", StringType.GLOBAL),

                SimplePropertyMetadata.of("sourceType", "sourceType", StringType.GLOBAL),
                SimplePropertyMetadata.of("sourceName", "sourceName", StringType.GLOBAL),
                SimplePropertyMetadata.of("sourceId", "sourceId", StringType.GLOBAL),

                SimplePropertyMetadata.of("alarmInfo", "alarmInfo", longStringType),
                SimplePropertyMetadata.of("creatorId", "creatorId", StringType.GLOBAL),
                SimplePropertyMetadata.of("termSpec", "termSpec", longStringType),
                SimplePropertyMetadata.of("triggerDesc", "triggerDesc", longStringType),
                SimplePropertyMetadata.of("actualDesc", "actualDesc", longStringType),
                SimplePropertyMetadata.of("alarmConfigSource", "alarmConfigSource", StringType.GLOBAL),
                SimplePropertyMetadata.of("bindings", "bindings", new ArrayType().elementType(StringType.GLOBAL))

            )).block(Duration.ofSeconds(10));
    }
}
