package org.jetlinks.community.logging.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
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

    public SystemLoggerEventHandler(ElasticSearchService elasticSearchService, IndexOperationService indexOperationService) {
        this.elasticSearchService = elasticSearchService;

        CreateIndexRequest systemLoggerIndex = CreateIndex.createInstance()
            .addIndex(LoggerIndexProvider.SYSTEM.getStandardIndex())
            .createMapping()
            .addFieldName("createTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
            .addFieldName("name").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("level").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("message").addFieldType(FieldType.KEYWORD).commit()
            .end()
            .createIndexRequest();

        indexOperationService.init(systemLoggerIndex)
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe();
    }

    @EventListener
    public void acceptAccessLoggerInfo(SerializableSystemLog info) {
        elasticSearchService.commit(LoggerIndexProvider.SYSTEM, Mono.just(info))
            .subscribe();
    }

}
