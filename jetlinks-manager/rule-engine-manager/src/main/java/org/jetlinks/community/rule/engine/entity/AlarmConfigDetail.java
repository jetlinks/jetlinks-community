package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.scene.internal.triggers.ManualTriggerProvider;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警配置详情.
 *
 * @author zhangji 2022/12/13
 */
@Getter
@Setter
public class AlarmConfigDetail {

    @Schema(description = "告警配置ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "告警目标类型")
    private String targetType;

    @Schema(description = "告警级别")
    private Integer level;

    @Schema(description = "关联场景")
    private List<SceneInfo> scene;

    @Schema(description = "状态")
    private AlarmState state;

    @Schema(description = "场景触发类型")
    private String sceneTriggerType;

    @Schema(description = "说明")
    private String description;

    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Schema(
            description = "创建者名称(只读)"
            , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorName;

    @Schema(description = "更新者ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String modifierId;

    @Schema(description = "更新时间")
    private Long modifyTime;

    @Schema(description = "修改人名称")
    private String modifierName;

    public static AlarmConfigDetail of(AlarmConfigEntity entity) {
        return FastBeanCopier.copy(entity, new AlarmConfigDetail(), "sceneTriggerType");
    }

    public AlarmConfigDetail withScene(List<SceneEntity> sceneEntityList) {
        List<SceneInfo> sceneList = new ArrayList<>();
        for (SceneEntity sceneEntity : sceneEntityList) {
            sceneList.add(SceneInfo.of(sceneEntity));
            String triggerType = sceneEntity.getTriggerType();
            // 存在一个手动触发场景，则将告警配置视为手动触发类型
            if (this.sceneTriggerType == null || ManualTriggerProvider.PROVIDER.equals(triggerType)) {
                this.sceneTriggerType = triggerType;
            }
        }
        this.scene = sceneList;
        return this;
    }

    /**
     * 场景联动信息
     */
    @Getter
    @Setter
    public static class SceneInfo {
        @Schema(description = "场景联动ID")
        private String id;

        @Column(nullable = false)
        @Schema(description = "场景联动名称")
        @NotBlank
        private String name;

        @Schema(description = "触发器类型")
        private String triggerType;

        @Schema(description = "状态")
        private RuleInstanceState state;

        public static SceneInfo of(SceneEntity entity) {
            return FastBeanCopier.copy(entity, new SceneInfo());
        }
    }
}
