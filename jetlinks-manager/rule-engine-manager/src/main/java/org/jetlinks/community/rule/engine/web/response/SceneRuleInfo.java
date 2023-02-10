package org.jetlinks.community.rule.engine.web.response;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.entity.RuleInstanceEntity;
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

    public static SceneRuleInfo of(RuleInstanceEntity instance) {

        SceneRuleInfo info = FastBeanCopier.copy(JSON.parseObject(instance.getModelMeta()), new SceneRuleInfo());
        info.setState(instance.getState());
        info.setId(instance.getId());
        info.setCreateTime(info.getCreateTime());
        return info;
    }
}
