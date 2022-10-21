package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class SceneConditionAction implements Serializable {

    @Schema(description = "条件")
    private List<Term> when;

    @Schema(description = "防抖配置")
    private ShakeLimit shakeLimit;

    @Schema(description = "满足条件时执行的动作")
    private SceneActions then;

}
