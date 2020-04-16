package org.jetlinks.community.rule.engine.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.rule.engine.event.handler.RuleEngineLoggerIndexProvider;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Order(1)
@Slf4j
public class RuleEngineLogIndexInitialize {

    public RuleEngineLogIndexInitialize(ElasticSearchIndexManager indexManager) {
        indexManager.putIndex(new DefaultElasticSearchIndexMetadata(RuleEngineLoggerIndexProvider.RULE_LOG.getIndex())
            .addProperty("createTime", new DateTimeType())
            .addProperty("level", new StringType())
            .addProperty("message", new StringType())
            .addProperty("nodeId", new StringType())
            .addProperty("instanceId", new StringType()))
            .then(
                indexManager.putIndex(new DefaultElasticSearchIndexMetadata(RuleEngineLoggerIndexProvider.RULE_EVENT_LOG.getIndex())
                    .addProperty("createTime", new DateTimeType())
                    .addProperty("event", new StringType())
                    .addProperty("nodeId", new StringType())
                    .addProperty("ruleData",new StringType())
                    .addProperty("instanceId", new StringType()))
            )
            .subscribe();
    }

}
