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
package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class SceneConditionAction implements Serializable {

    /**
     * @see org.jetlinks.community.rule.engine.scene.term.TermColumn
     * @see org.jetlinks.community.reactorql.term.TermType
     * @see org.jetlinks.community.reactorql.term.TermValue
     */
    @Schema(description = "条件")
    private List<Term> when;

    @Schema(description = "防抖配置")
    private ShakeLimit shakeLimit;

    @Schema(description = "满足条件时执行的动作")
    private List<SceneActions> then;

    @Schema(description = "无论如何都尝试执行此分支")
    private boolean executeAnyway = false;

    @Schema(description = "分支ID")
    private Integer branchId;

    @Schema(description = "分支名称")
    private String branchName;

    @Schema(description = "拓展信息")
    private Map<String, Object> options;

    //仅用于设置到reactQl sql的column中
    public List<Term> createContextTerm() {
        List<Term> contextTerm = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(then)) {
            for (SceneActions sceneActions : then) {
                contextTerm.addAll(sceneActions
                                       .createContextColumns()
                                       .stream()
                                       .map(column -> {
                                           Term term = new Term();
                                           term.setColumn(column);
                                           return term;
                                       })
                                       .collect(Collectors.toList()));
            }
        }
        if (CollectionUtils.isNotEmpty(when)) {
            contextTerm.addAll(when);
        }
        // 分支触发条件需要查询的列
        contextTerm.addAll(SceneAction
                               .parseColumnFromOptions(options)
                               .stream()
                               .map(column -> {
                                   Term term = new Term();
                                   term.setColumn(column);
                                   return term;
                               })
                               .collect(Collectors.toList()));
        return contextTerm;
    }
}
