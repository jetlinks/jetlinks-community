package org.jetlinks.community.notify.manager.service;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;

@AllArgsConstructor
@Component
public class ElasticSearchNotifyHistoryRepository implements NotifyHistoryRepository {

    public static final String INDEX_NAME = "notify_history";

    private final ElasticSearchService elasticSearchService;
    private final ElasticSearchIndexManager indexManager;

    @PostConstruct
    public void init() {
        indexManager
            .putIndex(
                new DefaultElasticSearchIndexMetadata(INDEX_NAME)
                    .addProperty("id", StringType.GLOBAL)
                    .addProperty("notifyTime", DateTimeType.GLOBAL)
                //其他字段默认都是string
            )
            .block(Duration.ofSeconds(10));
    }

    @Subscribe("/notify/**")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> handleNotify(SerializableNotifierEvent event) {
        return elasticSearchService
            .commit(INDEX_NAME, NotifyHistory.of(event).toJson());
    }


    @Override
    public Mono<PagerResult<NotifyHistory>> queryPager(QueryParamEntity param) {
        return elasticSearchService
            .queryPager(INDEX_NAME, param, map -> {
                NotifyHistory history = FastBeanCopier.copy(map, new NotifyHistory(),"context");
                history.setContext(JSON.parseObject((String) map.getOrDefault("context", "{}")));
                return history;
            });
    }
}
