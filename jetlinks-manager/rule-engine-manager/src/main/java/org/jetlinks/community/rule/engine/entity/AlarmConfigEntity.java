package org.jetlinks.community.rule.engine.entity;

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
import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.community.rule.engine.scene.TriggerType;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Table(name = "alarm_config", indexes = {
    @Index(name = "idx_ac_scene_id", columnList = "scene_id")
})
@Comment("告警配置表")
@EnableEntityEvent
public class AlarmConfigEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 64, nullable = false)
    @Schema(description = "名称")
    private String name;

    @Column(length = 64, nullable = false)
    @Schema(description = "告警目标类型")
    private String targetType;

    @Column(nullable = false)
    @Schema(description = "告警级别")
    private Integer level;

    @Column(length = 128)
    @Schema(description = "关联场景名称")
    @Deprecated
    private String sceneName;

    @Column(length = 64)
    @Schema(description = "关联场景Id")
    @Deprecated
    private String sceneId;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("enabled")
    @Schema(description = "状态")
    private AlarmState state;

    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "场景触发类型")
    @Deprecated
    private TriggerType sceneTriggerType;

    @Column(length = 256)
    @Schema(description = "说明")
    private String description;

    @Column(length = 64, updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Column(name = "creator_name", updatable = false)
    @Schema(
            description = "创建者名称(只读)"
            , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorName;

    @Column(length = 64)
    @Schema(description = "更新者ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String modifierId;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "更新时间")
    private Long modifyTime;

    @Column(length = 64)
    @Schema(description = "修改人名称")
    private String modifierName;


    public Map<String, Object> toConfigMap() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(AlarmConstants.ConfigKey.alarmConfigId, getId());
        configs.put(AlarmConstants.ConfigKey.alarmName, getName());
        configs.put(AlarmConstants.ConfigKey.level, getLevel());
        configs.put(AlarmConstants.ConfigKey.ownerId, getModifierId() == null ? getCreatorId() : getModifierId());
        configs.put(AlarmConstants.ConfigKey.targetType, getTargetType());
        configs.put(AlarmConstants.ConfigKey.state, getState().name());
        configs.put(AlarmConstants.ConfigKey.description, getDescription());
        return configs;
    }
}
