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
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskSnapshot;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "rule_task_snapshot",indexes = {
    @Index(name = "idx_rtsk_sid",columnList = "schedulerId"),
    @Index(name = "idx_rtsk_inid",columnList = "instanceId,nodeId")
})
@Comment("规则快照表")
@Generated
public class TaskSnapshotEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false)
    @Schema(description = "规则实例ID")
    private String instanceId;

    @Column(length = 128, nullable = false)
    @Schema(description = "调度ID")
    private String schedulerId;

    @Column(length = 128, nullable = false)
    @Schema(description = "执行ID")
    private String workerId;

    @Column(length = 64, nullable = false)
    @Schema(description = "节点ID")
    private String nodeId;

    @Column(length = 64, nullable = false)
    @Schema(description = "执行器")
    private String executor;

    @Column
    @Schema(description = "名称")
    private String name;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "调度任务")
    private ScheduleJob job;

    @Column(nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "开始时间")
    private Long startTime;

    @Column(nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "最近状态更新时间")
    private Long lastStateTime;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("shutdown")
    @Schema(description = "状态")
    private Task.State state;

    public TaskSnapshot toSnapshot(){
        return FastBeanCopier.copy(this,new TaskSnapshot());
    }

    public static TaskSnapshotEntity of(TaskSnapshot snapshot) {
        TaskSnapshotEntity entity = FastBeanCopier.copy(snapshot, new TaskSnapshotEntity());
        entity.setExecutor(snapshot.getJob().getExecutor());
        entity.setNodeId(snapshot.getJob().getNodeId());

        return entity;
    }
}
