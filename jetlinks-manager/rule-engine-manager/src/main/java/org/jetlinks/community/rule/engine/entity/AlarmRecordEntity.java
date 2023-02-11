package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "alarm_record")
@Comment("告警记录")
public class AlarmRecordEntity extends GenericEntity<String> {


    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "告警配置ID")
    private String alarmConfigId;

    @Column(length = 64, nullable = false)
    @Schema(description = "告警配置名称")
    private String alarmName;

    @Column(length = 32, updatable = false)
    @Schema(description = "告警目标类型")
    private String targetType;

    @Column(length = 64, updatable = false)
    @Schema(description = "告警目标Id")
    private String targetId;

    @Column(length = 64, updatable = false)
    @Schema(description = "告警目标Key")
    private String targetKey;

    @Column
    @Schema(description = "告警目标名称")
    private String targetName;

    @Column(length = 32)
    @Schema(description = "告警源类型")
    private String sourceType;

    @Column(length = 64)
    @Schema(description = "告警源Id")
    private String sourceId;

    @Column
    @Schema(description = "告警源名称")
    private String sourceName;


    @Column
    @Schema(description = "最近一次告警时间")
    private Long alarmTime;

    @Column
    @Schema(description = "处理时间")
    private Long handleTime;

    @Column
    @Schema(description = "告警级别")
    private Integer level;

    @Column(length = 32)
    @Schema(description = "告警记录状态")
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("normal")
    private AlarmRecordState state;

    @Column
    @Schema(description = "说明")
    private String description;

    public String getTargetKey() {
        if (targetKey == null) {
            generateTargetKey();
        }
        return targetKey;
    }

    public void generateTargetKey() {
        setTargetKey(generateId(targetId, targetType));
    }

    public void generateId() {
        setId(generateId(targetId, targetType, alarmConfigId));
    }

    public static String generateId(String... args) {
        return DigestUtils.md5Hex(String.join("-", args));
    }


}
