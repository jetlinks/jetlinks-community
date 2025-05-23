/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.rule.engine.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.dictionary.Dictionary;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.enums.AlarmHandleState;
import org.jetlinks.community.rule.engine.service.AlarmHandleTypeDictInit;
import org.jetlinks.community.terms.TermSpec;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

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

    @Column(length = 256, nullable = false)
    @Schema(description = "说明")
    private String description;

    @Column
    @Schema(description = "处理时间")
    private Long handleTime;

    @Column(updatable = false)
    @Schema(description = "告警时间")
    private Long alarmTime;

    @Column(length = 32)
    @Schema(description = "告警处理状态")
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("unprocessed")
    private AlarmHandleState state;

    @Column(length = 32, updatable = false)
    @Schema(description = "告警目标类型")
    private String targetType;

    @Column(length = 64, updatable = false)
    @Schema(description = "告警目标Id")
    private String targetId;

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
    @Schema(description = "告警级别")
    private Integer level;

    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @JsonCodec
    @Schema(description = "触发条件")
    private TermSpec termSpec;

    @Column(length = 1024)
    @Schema(description = "触发条件描述")
    private String triggerDesc;

    @Column(length = 1024)
    @Schema(description = "告警原因描述")
    private String actualDesc;

    /**
     * 告警流水。每次告警到结束告警唯一
     */
    @Column
    @Schema(description = "告警流水号")
    private String serialNumber;


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


    public static String generateId(String... args) {
        return DigestUtils.md5Hex(String.join("-", args));
    }

    public void generateId() {
        if (alarmTime != null) {
            setId(generateId(alarmRecordId, alarmTime.toString()));
        }
    }

    @SuppressWarnings("all")
    public static AlarmHandleHistoryEntity of(AlarmHandleInfo handleInfo) {
        AlarmHandleHistoryEntity entity = FastBeanCopier.copy(handleInfo, new AlarmHandleHistoryEntity());
        entity.setAlarmId(handleInfo.getAlarmConfigId());
        String type = handleInfo.getType();
        if (StringUtils.hasText(type)) {
            entity.setHandleType(EnumDict.create(type));
        }
        entity.setDescription(handleInfo.getDescribe() == null ? "" : handleInfo.getDescribe());
        entity.setState(handleInfo.getHandleState());
        entity.generateId();
        return entity;
    }
}
