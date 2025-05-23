/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.scene.SceneRule;
import org.jetlinks.community.rule.engine.web.request.SceneExecuteRequest;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleEngine;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class SceneService extends GenericReactiveCrudService<SceneEntity, String> {

    private final RuleEngine ruleEngine;

    public Mono<Void> execute(String id, Map<String, Object> data) {
        long t = System.currentTimeMillis();
        data.put("_now", t);
        data.put("timestamp", t);

        return ruleEngine
            .getTasks(id)
            .filter(task -> task.getJob().getNodeId().equals(id))
            .next()//只执行一个
            .flatMap(task -> task.execute(RuleData.create(data)))
            .then();
    }

    public Mono<Void> executeBatch(Flux<SceneExecuteRequest> requestFlux) {
        long t = System.currentTimeMillis();

        return requestFlux
            .doOnNext(request -> {
                if (request.getContext() == null) {
                    request.setContext(new HashMap<>());
                }
                request.getContext().put("_now", t);
                request.getContext().put("timestamp", t);
            })
            .flatMap(request -> ruleEngine
                .getTasks(request.getId())
                .filter(task -> task.getJob().getNodeId().equals(request.getId()))
                .next()//只执行一个
                .flatMap(task -> task.execute(RuleData.create(request.getContext()))))
            .then();
    }

    @Transactional(rollbackFor = Throwable.class)
    public Mono<SceneEntity> createScene(SceneRule rule) {
        if (!StringUtils.hasText(rule.getId())) {
            rule.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
        }
        rule.validate();
        SceneEntity entity = new SceneEntity().with(rule);
        entity.setState(RuleInstanceState.disable);

        return this
            .insert(entity)
            .thenReturn(entity);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Mono<SceneEntity> updateScene(String id, SceneRule rule) {
        rule.setId(id);
        rule.validate();
        SceneEntity entity = new SceneEntity().with(rule);

        return this
            .updateById(id, entity)
            .thenReturn(entity);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Mono<Void> enable(String id) {
        Assert.hasText(id, "id can not be empty");
        long now = System.currentTimeMillis();
        return this
            .createUpdate()
            .set(SceneEntity::getState, RuleInstanceState.started)
            .set(SceneEntity::getModifyTime, now)
            .set(SceneEntity::getStartTime, now)
            .where(SceneEntity::getId, id)
            .execute()
            .then();
    }

    @Transactional
    public Mono<Void> disabled(String id) {
        Assert.hasText(id, "id can not be empty");
        return this
            .createUpdate()
            .set(SceneEntity::getState, RuleInstanceState.disable)
            .where(SceneEntity::getId, id)
            .execute()
            .then();
    }

    @EventListener
    public void handleSceneSaved(EntitySavedEvent<SceneEntity> event) {
        event.async(
            handleEvent(event.getEntity())
        );
    }

    @EventListener
    public void handleSceneSaved(EntityModifyEvent<SceneEntity> event) {
        event.async(
            handleEvent(event.getAfter())
        );
    }

    @EventListener
    public void handleSceneSaved(EntityCreatedEvent<SceneEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .map(SceneEntity::getId)
                .as(this::findById)
                .collectList()
                .flatMap(this::handleEvent)
        );
    }

    private Mono<Void> handleEvent(Collection<SceneEntity> entities) {
        return Flux
            .fromIterable(entities)
            .flatMap(scene -> {
                //禁用时,停止规则
                if (scene.getState() == RuleInstanceState.disable) {
                    return ruleEngine.shutdown(scene.getId());
                } else if (scene.getState() == RuleInstanceState.started) {
                    scene.validate();
                    return scene
                        .toRule()
                        .flatMap(instance -> ruleEngine.startRule(scene.getId(), instance.getModel()).then())
                        ;
                }
                return Mono.empty();
            })
            .then();
    }

    @EventListener
    public void handleSceneDelete(EntityDeletedEvent<SceneEntity> event) {
        for (SceneEntity entity : event.getEntity()) {
            entity.setState(RuleInstanceState.disable);
        }
        event.async(
            handleEvent(event.getEntity())
        );
    }

}
