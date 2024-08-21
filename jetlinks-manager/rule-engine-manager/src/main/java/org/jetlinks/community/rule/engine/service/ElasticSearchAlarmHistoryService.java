package org.jetlinks.community.rule.engine.service;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bestfeng
 */
@AllArgsConstructor
public class ElasticSearchAlarmHistoryService implements AlarmHistoryService {


    public final static String ALARM_HISTORY_INDEX = "alarm_history";

    private final ElasticSearchIndexManager indexManager;

    private final ElasticSearchService elasticSearchService;


    public Mono<PagerResult<AlarmHistoryInfo>> queryPager(QueryParam queryParam) {
        return elasticSearchService.queryPager(ALARM_HISTORY_INDEX, queryParam, AlarmHistoryInfo.class);
    }

    public Mono<Void> save(AlarmHistoryInfo historyInfo) {
        return elasticSearchService.commit(ALARM_HISTORY_INDEX, createData(historyInfo));
    }

    public Mono<Void> save(Flux<AlarmHistoryInfo> historyInfo) {
        return elasticSearchService.save(ALARM_HISTORY_INDEX, historyInfo.map(this::createData));
    }

    public Mono<Void> save(Mono<AlarmHistoryInfo> historyInfo) {
        return elasticSearchService.save(ALARM_HISTORY_INDEX, historyInfo.map(this::createData));
    }

    private Map<String, Object> createData(AlarmHistoryInfo info) {
        Map<String, Object> data = FastBeanCopier.copy(info, new HashMap<>(16), "termSpec");
        if (info.getTermSpec() != null) {
            data.put("termSpec", JSONObject.toJSONString(info.getTermSpec()));
        }
        return data;
    }

    public void init() {
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

                .addProperty("sourceType", StringType.GLOBAL)
                .addProperty("sourceName", StringType.GLOBAL)
                .addProperty("sourceId", StringType.GLOBAL)

                .addProperty("alarmInfo", StringType.GLOBAL)
                .addProperty("creatorId", StringType.GLOBAL)
                .addProperty("termSpec", StringType.GLOBAL)
                .addProperty("triggerDesc", StringType.GLOBAL)
                .addProperty("actualDesc", StringType.GLOBAL)
                .addProperty("alarmConfigSource", StringType.GLOBAL)
                .addProperty("bindings", new ArrayType().elementType(StringType.GLOBAL))
        ).block(Duration.ofSeconds(10));
    }
}
