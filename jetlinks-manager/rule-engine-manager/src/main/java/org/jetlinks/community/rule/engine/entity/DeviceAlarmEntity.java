package org.jetlinks.community.rule.engine.entity;

import com.alibaba.fastjson.JSON;
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
    private String target;

    //deviceId or productId
    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[targetId]不能为空")
    private String targetId;

    //名称
    @Column
    private String name;

    //说明
    @Column
    private String description;

    //规则
    @Column(nullable = false)
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    private DeviceAlarmRule alarmRule;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("stopped")
    private AlarmState state;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
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
