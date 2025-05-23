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
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.community.rule.engine.alarm.AlarmLevelInfo;
import org.jetlinks.community.rule.engine.service.AlarmLevelService;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;

@Getter
@Setter
@Table(name = "alarm_level")
@Comment("告警级别")
public class AlarmLevelEntity extends GenericEntity<String> {

    @Column(length = 64)
    @Schema(description = "名称")
    private String name;

    @Column(name = "config")
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @JsonCodec
    @Schema(description = "配置信息")
    private List<AlarmLevelInfo> levels;

    @Column(length = 256)
    @Schema(description = "说明")
    private String description;

    public static AlarmLevelEntity of(List<AlarmLevelInfo> levels){
        AlarmLevelEntity entity = new AlarmLevelEntity();
        entity.setLevels(levels);
        return entity;
    }

    public static AlarmLevelEntity defaultOf(List<AlarmLevelInfo> levels){
        AlarmLevelEntity entity = new AlarmLevelEntity();
        entity.setId(AlarmLevelService.DEFAULT_ALARM_ID);
        entity.setName("default");
        entity.setDescription("default");
        entity.setLevels(levels);
        return entity;
    }
}
