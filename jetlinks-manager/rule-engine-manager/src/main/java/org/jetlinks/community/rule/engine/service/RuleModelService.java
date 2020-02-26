package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.rule.engine.entity.RuleModelEntity;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RuleModelService extends GenericReactiveCrudService<RuleModelEntity, String> {

    @Autowired
    private RuleInstanceService instanceService;

    @Autowired
    private RuleEngineModelParser modelParser;


    public Mono<Boolean> deploy(String id) {
        return findById(Mono.just(id))
                .map(RuleModelEntity::toInstance)
                .doOnNext(instance -> modelParser.parse(instance.getModelType(), instance.getModelMeta()))
                .flatMap(rule -> instanceService.save(Mono.just(rule)))
                .thenReturn(true);
    }

    @Override
    public Mono<Integer> updateById(String id, Mono<RuleModelEntity> entityPublisher) {
        return entityPublisher
                .flatMap(model -> {
                    model.setId(id);
                    model.setVersion(null);
                    return createUpdate()
                            .set(model)
                            .set(RuleModelEntity::getVersion, NativeSql.of("version+1"))
                            .where(model::getId)
                            .execute();
                });
    }

    @Override
    public Mono<SaveResult> save(Publisher<RuleModelEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                .flatMap(rule -> {
                    rule.setVersion(null);
                    return createUpdate()
                            .set(rule)
                            .set(RuleModelEntity::getVersion, NativeSql.of("version+1"))
                            .where(rule::getId)
                            .execute()
                            .filter(i -> i > 0)
                            .map(i -> SaveResult.of(0, i))
                            .switchIfEmpty(Mono.defer(() -> insert(Mono.just(rule))
                                    .map(i -> SaveResult.of(1, 0))));

                }).reduce(SaveResult.of(0, 0), SaveResult::merge);
    }


    @Override
    public Mono<Integer> insert(Publisher<RuleModelEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                .map(rule -> {
                    if (rule.getVersion() == null) {
                        rule.setVersion(1);
                    }
                    return rule;
                }).as(super::insert)
                .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("模型ID重复", err));
    }
}
