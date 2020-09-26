package org.jetlinks.community.rule.engine.entity;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.community.rule.engine.device.DeviceAlarmRule;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.community.rule.engine.model.DeviceAlarmModelParser;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.Date;

@Getter
@Setter
@Table(name = "rule_dev_alarm", indexes = {
    @Index(name = "idx_rda_tid", columnList = "target,target_id")
})
public class DeviceAlarmEntity extends GenericEntity<String> {

    //device or product
    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[target]不能为空")
    @Schema(description = "device或者product")
    private String target;

    //deviceId or productId
    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(message = "[targetId]不能为空")
    @Schema(description = "deviceId或者productId")
    private String targetId;

    //名称
    @Column
    @Schema(description = "告警配置名称")
    private String name;

    //说明
    @Column
    @Schema(description = "说明")
    private String description;

    //规则
    @Column(nullable = false)
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "规则配置")
    private DeviceAlarmRule alarmRule;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("stopped")
    @Schema(description = "状态")
    private AlarmState state;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)")
    private Date createTime;

    public RuleInstanceEntity toRuleInstance() {
        RuleInstanceEntity instanceEntity = new RuleInstanceEntity();
        if (alarmRule == null) {
            throw new IllegalArgumentException("未设置告警规则");
        }
        alarmRule.setId(getId());
        alarmRule.setName(name);

        instanceEntity.setModelVersion(1);
        instanceEntity.setId(getId());
        instanceEntity.setModelId(getId());
        instanceEntity.setDescription(description);
        instanceEntity.setModelType(DeviceAlarmModelParser.format);
        instanceEntity.setName("设备告警:" + name);
        instanceEntity.setModelMeta(JSON.toJSONString(this));
        instanceEntity.setCreateTimeNow();
        return instanceEntity;
    }
}
