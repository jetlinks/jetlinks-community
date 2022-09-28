package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
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

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "告警配置名称")
    private String alarmName;

    @Column
    @Schema(description = "告警目标类型")
    private String targetType;

    @Column
    @Schema(description = "告警目标Id")
    private String targetId;

    @Column
    @Schema(description = "告警目标名称")
    private String targetName;

    @Column
    @Schema(description = "最近一次告警时间")
    private Long alarmTime;

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


    public void generateId() {
        setId(generateId(targetId, targetType, alarmConfigId));
    }

    public static String generateId(String targetId, String targetType, String alarmConfigId) {
        return DigestUtils.md5Hex(String.join("-", targetId, targetType, alarmConfigId));
    }


}
