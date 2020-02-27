package org.jetlinks.community.rule.engine.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.jetlinks.community.rule.engine.event.handler.RuleEngineLoggerIndexProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Order(1)
@Slf4j
public class RuleEngineLogIndexInitialize implements CommandLineRunner {

    private final IndexOperationService indexOperationService;

    @Autowired
    public RuleEngineLogIndexInitialize(IndexOperationService indexOperationService) {
        this.indexOperationService = indexOperationService;
    }


    @Override
    public void run(String... args) throws Exception {
        indexOperationService.init(CreateIndex.createInstance()
            .addIndex(RuleEngineLoggerIndexProvider.RULE_LOG.getStandardIndex())
            .createMapping()
            .addFieldName("createTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
            .addFieldName("level").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("message").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("nodeId").addFieldType(FieldType.KEYWORD).commit()
            .addFieldName("instanceId").addFieldType(FieldType.KEYWORD).commit()
            .end()
            .createIndexRequest())
            .and(
                indexOperationService.init(CreateIndex.createInstance()
                    .addIndex(RuleEngineLoggerIndexProvider.RULE_EVENT_LOG.getStandardIndex())
                    .createMapping()
                    .addFieldName("createTime").addFieldType(FieldType.DATE).addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time).commit()
                    .addFieldName("event").addFieldType(FieldType.KEYWORD).commit()
                    .addFieldName("nodeId").addFieldType(FieldType.KEYWORD).commit()
                    .addFieldName("instanceId").addFieldType(FieldType.KEYWORD).commit()
                    .end()
                    .createIndexRequest())
            )
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe();
    }
}
