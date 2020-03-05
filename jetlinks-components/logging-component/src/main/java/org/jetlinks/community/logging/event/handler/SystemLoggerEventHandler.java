package org.jetlinks.community.logging.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.logging.system.SerializableSystemLog;
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
public class SystemLoggerEventHandler {


    private final ElasticSearchService elasticSearchService;

    public SystemLoggerEventHandler(ElasticSearchService elasticSearchService, ElasticSearchIndexManager indexManager) {
        this.elasticSearchService = elasticSearchService;

        indexManager.putIndex(
            new DefaultElasticSearchIndexMetadata(LoggerIndexProvider.SYSTEM.getIndex())
                .addProperty("createTime", new DateTimeType())
                .addProperty("name", new StringType())
                .addProperty("level", new StringType())
                .addProperty("message", new StringType())
                .addProperty("context", new ObjectType()
                    .addProperty("requestId",new StringType())
                    .addProperty("server",new StringType()))
        ).subscribe();
    }

    @EventListener
    public void acceptAccessLoggerInfo(SerializableSystemLog info) {
        elasticSearchService.commit(LoggerIndexProvider.SYSTEM, Mono.just(info))
            .subscribe();
    }

}
