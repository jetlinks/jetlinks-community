package org.jetlinks.community.rule.engine.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.event.handler.RuleEngineLoggerIndexProvider;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RuleInstanceService extends GenericReactiveCrudService<RuleInstanceEntity, String> {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleEngineModelParser modelParser;

    @Autowired
    private ElasticSearchService elasticSearchService;

    public Mono<PagerResult<RuleEngineExecuteEventInfo>> queryExecuteEvent(QueryParam queryParam) {
        return elasticSearchService.queryPager(RuleEngineLoggerIndexProvider.RULE_EVENT_LOG, queryParam, RuleEngineExecuteEventInfo.class);
    }

    public Mono<PagerResult<RuleEngineExecuteLogInfo>> queryExecuteLog(QueryParam queryParam) {
        return elasticSearchService.queryPager(RuleEngineLoggerIndexProvider.RULE_LOG, queryParam, RuleEngineExecuteLogInfo.class);
    }

    @Transactional
    public Mono<Void> stop(String id) {
        return this.ruleEngine
            .shutdown(id)
            .then(createUpdate()
                      .set(RuleInstanceEntity::getState, RuleInstanceState.disable)
                      .where(RuleInstanceEntity::getId, id)
                      .execute())
            .then();
    }

    @Transactional
    public Mono<Void> start(String id) {
        return findById(Mono.just(id))
            .flatMap(this::doStart);
    }

    private Mono<Void> doStart(RuleInstanceEntity entity) {
        return Mono.defer(() -> {
            RuleModel model = entity.toRule(modelParser);
            return ruleEngine
                .startRule(entity.getId(), model)
                .then(createUpdate()
                          .set(RuleInstanceEntity::getState, RuleInstanceState.started)
                          .where(entity::getId)
                          .execute()).then();
        });
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .flatMap(id -> this.stop(id).thenReturn(id))
                   .as(super::deleteById);
    }

}
