package org.jetlinks.community.logging.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
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

    public AccessLoggerEventHandler(ElasticSearchService elasticSearchService, IndexOperationService indexOperationService) {
        this.elasticSearchService = elasticSearchService;
        CreateIndexRequest accessLoggerIndex = CreateIndex.createInstance()
            .addIndex(LoggerIndexProvider.ACCESS.getStandardIndex())
            .createMapping()
            .addFieldName("requestTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
            .addFieldName("responseTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
            .addFieldName("action").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("ip").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("url").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("httpHeaders").addFieldType(FieldType.OBJECT).commit()
            .addFieldName("context").addFieldType(FieldType.OBJECT).commit()
            .end()
            .createIndexRequest();
        indexOperationService.init(accessLoggerIndex)
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe();
    }


    @EventListener
    public void acceptAccessLoggerInfo(SerializableAccessLog info) {
        elasticSearchService.commit(LoggerIndexProvider.ACCESS, Mono.just(info)).subscribe();
    }


}
