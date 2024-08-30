package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.rule.engine.RuleEngineConstants;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.cluster.RuleInstance;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "rule_instance")
@Comment("规则实例表")
@EnableEntityEvent
public class RuleInstanceEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Override
    @GeneratedValue(generator = "snow_flake")
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    @Column(name = "model_id", length = 64)
    @Schema(description = "模型ID")
    private String modelId;

    @Column(name = "name")
    @Schema(description = "名称")
    private String name;

    @Column(name = "description")
    @Schema(description = "说明")
    private String description;

    @Column(name = "model_type")
    @Schema(description = "规则类型")
    private String modelType;

    @Column(name = "model_meta")
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "规则模型配置,不同的类型配置不同.")
    private String modelMeta;

    @Column(name = "model_version", nullable = false)
    @Schema(description = "版本")
    @DefaultValue("1")
    private Integer modelVersion;

    @Column(name = "create_time")
    @Schema(description = "创建时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Column(name = "creator_id")
    @Schema(description = "创建者ID")
    private String creatorId;

    @Column(length = 64)
    @Schema(
        description = "修改人ID"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String modifierId;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "修改时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long modifyTime;

    @Column(length = 64)
    @Schema(description = "修改人名称")
    private String modifierName;

    @Column(name = "state", length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("disable")
    @Schema(description = "状态")
    private RuleInstanceState state;

    @Comment("设备详情信息")
    @Column(name = "instance_detail_json")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Hidden
    private String instanceDetailJson;


    public RuleModel toRule(RuleEngineModelParser parser) {
        RuleModel model = parser.parse(modelType, modelMeta);
        model.setId(StringUtils.hasText(modelId) ? modelId : getId());
        model.setName(name);
        model.addConfiguration(RuleEngineConstants.ruleCreatorIdKey, creatorId);
        return model;
    }

    public RuleInstance toRuleInstance(RuleEngineModelParser parser) {
        RuleModel model = toRule(parser);
        RuleInstance instance = new RuleInstance();
        instance.setId(getId());
        instance.setModel(model);
        return instance;
    }
}
