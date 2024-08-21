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
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.community.dictionary.Dictionary;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.enums.AlarmHandleType;
import org.jetlinks.community.rule.engine.service.AlarmHandleTypeDictInit;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "alarm_handle_history", indexes = {
    @Index(name = "idx_ahh_alarm_record_id", columnList = "alarmRecordId")
})
@Comment("告警处理记录")
public class AlarmHandleHistoryEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "告警配置ID")
    private String alarmId;

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "告警记录Id")
    private String alarmRecordId;

    @Column(length = 32)
    @Dictionary(AlarmHandleTypeDictInit.DICT_ID)
    @Schema(description = "告警处理类型")
    @ColumnType(javaType = String.class)
    private EnumDict<String> handleType;

    @Column(length = 256, nullable = false, updatable = false)
    @Schema(description = "说明")
    private String description;

    @Column(updatable = false)
    @Schema(description = "处理时间")
    private Long handleTime;

    @Column(updatable = false)
    @Schema(description = "告警时间")
    private Long alarmTime;

    @Column(updatable = false)
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

    @SuppressWarnings("all")
    public static AlarmHandleHistoryEntity of(AlarmHandleInfo handleInfo) {
        AlarmHandleHistoryEntity entity = new AlarmHandleHistoryEntity();
        entity.setAlarmId(handleInfo.getAlarmConfigId());
        entity.setAlarmRecordId(handleInfo.getAlarmRecordId());
        entity.setAlarmTime(handleInfo.getAlarmTime());
        String type = handleInfo.getType();
        entity.setHandleType(StringUtils.hasText(type) ? EnumDict.create(type) : AlarmHandleType.system);
        entity.setDescription(handleInfo.getDescribe());
        entity.setHandleTime(handleInfo.getHandleTime() == null ? System.currentTimeMillis() : handleInfo.getHandleTime());
        return entity;
    }

}
