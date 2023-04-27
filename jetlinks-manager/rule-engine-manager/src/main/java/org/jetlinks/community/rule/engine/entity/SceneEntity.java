package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.rule.engine.RuleEngineConstants;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.community.rule.engine.scene.*;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.cluster.RuleInstance;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Table(name = "rule_scene")
@EnableEntityEvent
public class SceneEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(nullable = false)
    @Schema(description = "告警名称")
    @NotBlank
    private String name;

    @Schema(description = "触发器类型")
    @Column(length = 32, nullable = false, updatable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @NotNull
    private TriggerType triggerType;

    @Column
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "触发器")
    @NotNull
    private Trigger trigger;

    @Column
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "触发条件")
    private List<Term> terms;

    @Column
    @Schema(description = "是否并行执行动作")
    @DefaultValue("false")
    private Boolean parallel;

    @Column
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "执行动作")
    private List<SceneAction> actions;

    @Column
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "动作分支")
    private List<SceneConditionAction> branches;

    @Column(length = 64, updatable = false)
    @Schema(description = "创建人")
    private String creatorId;

    @Column(updatable = false)
    @Schema(description = "创建时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Column(length = 64)
    @Schema(description = "修改人")
    private String modifierId;

    @Column
    @Schema(description = "修改时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long modifyTime;

    @Column
    @Schema(description = "启动时间")
    private Long startTime;

    @Schema(description = "状态")
    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @NotBlank
    @DefaultValue("disable")
    private RuleInstanceState state;

    @Schema(description = "扩展配置")
    @Column(name = "options")
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    private Map<String, Object> options;

    @Column
    @Schema(description = "说明")
    private String description;

    public RuleInstance toRule() {
        SceneRule rule = copyTo(new SceneRule());

        RuleInstance instance = new RuleInstance();
        instance.setId(getId());
        RuleModel model = rule.toModel();
        model.addConfiguration(RuleEngineConstants.ruleCreatorIdKey, modifierId);
        model.addConfiguration(RuleEngineConstants.ruleName, getName());
        instance.setModel(model);
        return instance;
    }

    public SceneEntity with(SceneRule rule) {
        SceneEntity entity = copyFrom(rule);
        entity.setTriggerType(rule.getTrigger().getType());
        return entity;
    }


    public void validate() {
        getTrigger().validate();
       if (CollectionUtils.isEmpty(getActions()) && CollectionUtils.isEmpty(getBranches())){
           throw new BusinessException("error.scene_action_rule_cannot_be_null");
       }
    }
}
