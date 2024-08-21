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
import java.util.stream.Collectors;

@Getter
@Setter
public class SceneConditionAction implements Serializable {

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
        return contextTerm;
    }
}
