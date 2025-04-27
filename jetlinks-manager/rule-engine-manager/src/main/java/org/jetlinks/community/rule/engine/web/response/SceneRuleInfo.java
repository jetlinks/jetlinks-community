package org.jetlinks.community.rule.engine.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.scene.SceneRule;


@Getter
@Setter
public class SceneRuleInfo extends SceneRule {

    @Schema(description = "场景状态")
    private RuleInstanceState state;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "创建时间")
    private long createTime;

    public static SceneRuleInfo of(SceneEntity instance) {

        SceneRuleInfo info = FastBeanCopier.copy(instance, new SceneRuleInfo());
        info.setState(instance.getState());
        info.setId(instance.getId());
        info.setCreateTime(info.getCreateTime());
        return info;
    }
}
