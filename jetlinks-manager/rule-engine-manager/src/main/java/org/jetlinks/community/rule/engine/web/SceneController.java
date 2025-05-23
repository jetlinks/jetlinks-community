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
package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.scene.SceneRule;
import org.jetlinks.community.rule.engine.service.SceneService;
import org.jetlinks.community.rule.engine.web.request.SceneExecuteRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/scene")
@Tag(name = "场景管理")
@AllArgsConstructor
@Resource(id = "rule-scene", name = "场景管理")
public class SceneController implements ReactiveServiceQueryController<SceneEntity, String> {

    @Getter
    private final SceneService service;

    @PostMapping
    @Operation(summary = "创建场景")
    @SaveAction
    public Mono<SceneEntity> createScene(@RequestBody Mono<SceneRule> sceneRuleMono) {
        return sceneRuleMono
            .flatMap(service::createScene);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场景")
    @SaveAction
    public Mono<Void> update(@PathVariable String id,
                                  @RequestBody Mono<SceneRule> sceneRuleMono) {
        return sceneRuleMono
            .flatMap(sceneRule -> service.updateScene(id, sceneRule))
            .then();
    }

    @PutMapping("/{id}/_disable")
    @Operation(summary = "禁用场景")
    @SaveAction
    public Mono<Void> disableScene(@PathVariable String id) {
        return service.disabled(id);
    }

    @PutMapping("/{id}/_enable")
    @Operation(summary = "启用场景")
    @SaveAction
    public Mono<Void> enabledScene(@PathVariable String id) {
        return service.enable(id);
    }

    @PostMapping("/{id}/_execute")
    @Operation(summary = "手动执行场景")
    @SaveAction
    public Mono<Void> execute(@PathVariable String id,
                              @RequestBody Mono<Map<String, Object>> context) {
        return context.flatMap(ctx -> service.execute(id, ctx));
    }

    @PostMapping("/batch/_execute")
    @Operation(summary = "批量手动执行场景")
    @SaveAction
    public Mono<Void> executeBatch(@RequestBody Flux<SceneExecuteRequest> request) {
        return service.executeBatch(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除场景")
    @DeleteAction
    public Mono<Void> removeScene(@PathVariable String id) {
        return service
            .deleteById(id)
            .then();
    }

}
