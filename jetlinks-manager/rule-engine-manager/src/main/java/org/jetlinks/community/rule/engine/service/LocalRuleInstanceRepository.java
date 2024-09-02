package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.cluster.RuleInstance;
import org.jetlinks.rule.engine.cluster.RuleInstanceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
@AllArgsConstructor
@Slf4j
public class LocalRuleInstanceRepository implements RuleInstanceRepository, CommandLineRunner {
    private final RuleInstanceService instanceService;

    private final SceneService sceneService;

    private final RuleEngineModelParser parser;

    private final RuleEngine ruleEngine;

    @Nonnull
    @Override
    public Flux<RuleInstance> findAll() {
        return Flux
            .merge(
                instanceService
                    .createQuery()
                    .where(RuleInstanceEntity::getState, RuleInstanceState.started)
                    .fetch()
                    .flatMap(en -> Mono
                        .fromCallable(() -> en.toRuleInstance(parser))
                        .onErrorResume(err -> {
                            log.warn("convert rule instance [{}] error", en.getId(), err);
                            return Mono.empty();
                        })),
                sceneService
                    .createQuery()
                    .where(SceneEntity::getState, RuleInstanceState.started)
                    .fetch()
                    .flatMap(en -> Mono
                        .defer(en::toRule)
                        .onErrorResume(err -> {
                            log.warn("convert scene rule [{}] error", en.getId(), err);
                            return Mono.empty();
                        }))
            );
    }

    @Nonnull
    @Override
    public Flux<RuleInstance> findById(String id) {
        return Flux.merge(
            instanceService
                .createQuery()
                .where(RuleInstanceEntity::getId, id)
                .and(RuleInstanceEntity::getState, RuleInstanceState.started)
                .fetch()
                .map(en -> en.toRuleInstance(parser)),
            sceneService
                .createQuery()
                .where(SceneEntity::getId, id)
                .and(SceneEntity::getState, RuleInstanceState.started)
                .fetch()
                .flatMap(SceneEntity::toRule)
        )
            ;
    }

    @Override
    public void run(String... args) throws Exception {
        this
            .findAll()
            .flatMap(ruleInstance -> ruleEngine
                .startRule(ruleInstance.getId(), ruleInstance.getModel())
                .onErrorResume(err -> {
                    log.warn("启动规则[{}]失败: {}", ruleInstance.getModel().getName(), ruleInstance);
                    return Mono.empty();
                }))
            .subscribe();
    }
}
