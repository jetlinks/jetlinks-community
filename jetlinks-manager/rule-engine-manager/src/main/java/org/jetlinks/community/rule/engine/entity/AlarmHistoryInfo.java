package org.jetlinks.community.rule.engine.entity;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.alarm.AlarmTargetInfo;
import org.jetlinks.community.rule.engine.scene.SceneData;

import java.util.*;

@Getter
@Setter
public class AlarmHistoryInfo {

    @Schema(description = "id")
    private String id;

    @Schema(description = "告警配置ID")
    private String alarmConfigId;

    @Schema(description = "告警配置名称")
    private String alarmConfigName;

    @Schema(description = "告警记录ID")
    private String alarmRecordId;

    @Schema(description = "告警级别")
    private Integer level;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "告警时间")
    private Long alarmTime;

    @Schema(description = "告警目标类型")
    private String targetType;

    @Schema(description = "告警目标名称")
    private String targetName;

    @Schema(description = "告警目标Id")
    private String targetId;

    @Schema(description = "告警信息")
    private String alarmInfo;


    public static AlarmHistoryInfo of(String alarmRecordId,
                                      AlarmTargetInfo targetInfo,
                                      SceneData data,
                                      AlarmConfigEntity alarmConfig) {
        AlarmHistoryInfo info = new AlarmHistoryInfo();
        info.setAlarmConfigId(alarmConfig.getId());
        info.setAlarmConfigName(alarmConfig.getName());
        info.setAlarmRecordId(alarmRecordId);
        info.setLevel(alarmConfig.getLevel());
        info.setId(data.getId());
        info.setAlarmTime(System.currentTimeMillis());
        info.setTargetName(targetInfo.getTargetName());
        info.setTargetId(targetInfo.getTargetId());
        info.setTargetType(targetInfo.getTargetType());
        info.setAlarmInfo(JSON.toJSONString(data.getOutput()));
        info.setDescription(alarmConfig.getDescription());
        return info;
    }

}
