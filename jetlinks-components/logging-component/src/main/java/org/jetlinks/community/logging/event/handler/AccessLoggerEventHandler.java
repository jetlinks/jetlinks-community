package org.jetlinks.community.logging.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.logging.access.SerializableAccessLog;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Slf4j
@Order(5)
public class AccessLoggerEventHandler {

    private final ElasticSearchService elasticSearchService;


    public AccessLoggerEventHandler(ElasticSearchService elasticSearchService, ElasticSearchIndexManager indexManager) {
        this.elasticSearchService = elasticSearchService;
        indexManager.putIndex(
            new DefaultElasticSearchIndexMetadata(LoggerIndexProvider.ACCESS.getIndex())
                .addProperty("requestTime", new DateTimeType())
                .addProperty("responseTime", new DateTimeType())
                .addProperty("action", new StringType())
                .addProperty("ip", new StringType())
                .addProperty("url", new StringType())
                .addProperty("httpHeaders", new ObjectType())
                .addProperty("context", new ObjectType()
                    .addProperty("userId",new StringType())
                    .addProperty("username",new StringType())
                )
        ).subscribe();

    }


    @EventListener
    public void acceptAccessLoggerInfo(SerializableAccessLog info) {
        elasticSearchService.commit(LoggerIndexProvider.ACCESS, Mono.just(info)).subscribe();
    }


}
