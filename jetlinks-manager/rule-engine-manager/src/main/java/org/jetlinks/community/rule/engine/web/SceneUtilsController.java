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
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.reactorql.aggregation.AggregationSupport;
import org.jetlinks.community.rule.engine.scene.SceneRule;
import org.jetlinks.community.rule.engine.scene.SceneUtils;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.utils.TermColumnUtils;
import org.jetlinks.community.rule.engine.web.response.SceneActionInfo;
import org.jetlinks.community.rule.engine.web.response.SceneAggregationInfo;
import org.jetlinks.community.rule.engine.web.response.SceneTriggerInfo;
import org.jetlinks.community.rule.engine.web.response.SelectorInfo;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/scene")
@Tag(name = "场景管理")
@AllArgsConstructor
@Resource(id = "rule-scene", name = "场景管理")
public class SceneUtilsController {
    @GetMapping("/trigger/supports")
    @Operation(summary = "获取支持的触发器类型")
    public Flux<SceneTriggerInfo> getSupportTriggers() {
        return SceneUtils
                .getSupportTriggers()
                .map(SceneTriggerInfo::of);
    }

    @GetMapping("/action/supports")
    @Operation(summary = "获取支持的动作类型")
    public Flux<SceneActionInfo> getSupportActions() {
        return SceneUtils
                .getSupportActions()
                .flatMap(provider -> SceneActionInfo.of(provider));
    }

    @GetMapping("/aggregation/supports")
    @Operation(summary = "获取支持的聚合函数")
    public Flux<SceneAggregationInfo> getSupportAggregations() {
        return LocaleUtils
            .currentReactive()
            .flatMapMany(locale -> Flux
                .fromIterable(AggregationSupport.supports.getAll())
                .map(aggregation -> SceneAggregationInfo.of(aggregation, locale)));
    }

    @PostMapping("/parse-term-column")
    @Operation(summary = "根据触发器解析出支持的条件列")
    @QueryAction
    public Flux<TermColumn> parseTermColumns(@RequestBody Mono<SceneRule> ruleMono) {
        return ruleMono.flatMapMany(SceneUtils::parseTermColumns);
    }

    @PostMapping("/parse-array-child-term-column")
    @Operation(summary = "解析数组需要的子元素支持的条件列")
    @QueryAction
    public Flux<TermColumn> parseArrayChildTermColumns(@RequestBody Mono<SimplePropertyMetadata> metadataMono) {
        return metadataMono
            .flatMapMany(metadata -> Flux
                .fromIterable(TermColumnUtils.parseArrayChildTermColumns(metadata.getValueType())));
    }


    @PostMapping("/parse-variables")
    @Operation(summary = "解析规则中输出的变量")
    @QueryAction
    public Flux<Variable> parseVariables(@RequestBody Mono<SceneRule> ruleMono,
                                         @RequestParam(required = false) Integer branch,
                                         @RequestParam(required = false) Integer branchGroup,
                                         @RequestParam(required = false) Integer action) {
        return SceneUtils.parseVariables(ruleMono, branch, branchGroup, action);
    }

    @GetMapping("/device-selectors")
    @Operation(summary = "获取支持的设备选择器")
    @QueryAction
    public Flux<SelectorInfo> getDeviceSelectors() {
        return SceneUtils.getDeviceSelectors();
    }


}
