package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author bestfeng
 */
@AllArgsConstructor
public class ElasticSearchAlarmHistoryService implements AlarmHistoryService {

    public final static String ALARM_HISTORY_INDEX="alarm_history";

    private final ElasticSearchIndexManager indexManager;

    private final ElasticSearchService elasticSearchService;


    public Mono<PagerResult<AlarmHistoryInfo>> queryPager(QueryParam queryParam) {
        return elasticSearchService.queryPager(ALARM_HISTORY_INDEX, queryParam, AlarmHistoryInfo.class);
    }

    public Mono<Void> save(AlarmHistoryInfo historyInfo) {
        return elasticSearchService.save(ALARM_HISTORY_INDEX, historyInfo);
    }

    public Mono<Void> save(Flux<AlarmHistoryInfo> historyInfo) {
        return elasticSearchService.save(ALARM_HISTORY_INDEX, historyInfo);
    }

    public Mono<Void> save(Mono<AlarmHistoryInfo> historyInfo) {
        return elasticSearchService.save(ALARM_HISTORY_INDEX, historyInfo);
    }

    public void init(){
        indexManager.putIndex(
            new DefaultElasticSearchIndexMetadata(ALARM_HISTORY_INDEX)
                .addProperty("id", StringType.GLOBAL)
                .addProperty("alarmConfigId", StringType.GLOBAL)
                .addProperty("alarmConfigName", StringType.GLOBAL)
                .addProperty("alarmRecordId", StringType.GLOBAL)
                .addProperty("level", IntType.GLOBAL)
                .addProperty("description", StringType.GLOBAL)
                .addProperty("alarmTime", DateTimeType.GLOBAL)
                .addProperty("targetType", StringType.GLOBAL)
                .addProperty("targetName", StringType.GLOBAL)
                .addProperty("targetId", StringType.GLOBAL)
                .addProperty("alarmInfo", StringType.GLOBAL)
                .addProperty("creatorId", StringType.GLOBAL)
        ).block(Duration.ofSeconds(10));
    }
}
