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
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.dictionary.Dictionary;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.rule.engine.service.AlarmHandleTypeDictInit;
import org.jetlinks.community.terms.TermSpec;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "alarm_record", indexes = {
    @Index(name = "idx_alarm_rec_t_idtype", columnList = "targetId,targetType"),
    @Index(name = "idx_alarm_rec_t_id", columnList = "targetId"),
    @Index(name = "idx_alarm_rec_t_type", columnList = "targetType"),
    @Index(name = "idx_alarm_rec_t_key", columnList = "targetKey")
})
@Comment("告警记录")
public class AlarmRecordEntity extends GenericEntity<String> implements RecordCreationEntity {


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

    @Column
    @Schema(description = "当前首次告警时间")
    private Long alarmTime;

    @Column
    @Schema(description = "最近一次告警时间")
    private Long lastAlarmTime;

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

    @Column(length = 32)
    @Dictionary(AlarmHandleTypeDictInit.DICT_ID)
    @Schema(description = "告警处理类型")
    @ColumnType(javaType = String.class)
    private EnumDict<String> handleType;

    /**
     * 标识告警触发的配置来自什么业务功能
     */
    @Column
    @Schema(description = "告警配置源")
    private String alarmConfigSource;

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


    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Column(length = 64, updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;
}
