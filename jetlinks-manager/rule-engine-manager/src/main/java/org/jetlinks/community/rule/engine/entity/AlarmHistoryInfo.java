package org.jetlinks.community.rule.engine.entity;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.alarm.AlarmTargetInfo;
import org.jetlinks.community.rule.engine.scene.SceneData;
import org.jetlinks.community.terms.TermSpec;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
public class AlarmHistoryInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "告警数据ID")
    private String id;

    @Schema(description = "告警配置ID")
    private String alarmConfigId;

    @Schema(description = "告警配置名称")
    private String alarmConfigName;

    @Schema(description = "告警记录ID")
    private String alarmRecordId;

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "告警时间")
    private long alarmTime;

    @Schema(description = "告警目标类型")
    private String targetType;

    @Schema(description = "告警目标名称")
    private String targetName;

    @Schema(description = "告警目标Id")
    private String targetId;

    @Schema(description = "告警源类型")
    private String sourceType;

    @Schema(description = "告警源Id")
    private String sourceId;

    @Schema(description = "告警源名称")
    private String sourceName;

    @Schema(description = "告警信息")
    private String alarmInfo;

    @Schema(description = "创建者ID")
    private String creatorId;

    @Schema(description = "触发条件")
    private TermSpec termSpec;

    @Schema(description = "告警配置源")
    private String alarmConfigSource;

    @Schema(description = "触发条件描述")
    private String triggerDesc;

    @Schema(description = "告警原因描述")
    private String actualDesc;

    public void withTermSpec(TermSpec termSpec){
        if (termSpec != null) {
            this.setTermSpec(termSpec);
            this.setTriggerDesc(termSpec.getTriggerDesc());
            this.setActualDesc(termSpec.getActualDesc());
        }
    }

    @Deprecated
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

        info.setSourceName(targetInfo.getSourceName());
        info.setSourceType(targetInfo.getSourceType());
        info.setSourceId(targetInfo.getSourceId());

        info.setAlarmInfo(JSON.toJSONString(data.getOutput()));
        info.setDescription(alarmConfig.getDescription());
        return info;
    }

    @SuppressWarnings("all")
    @Deprecated
     static List<Map<String, Object>> convertBindings(AlarmTargetInfo targetInfo,
                                                     SceneData data,
                                                     AlarmConfigEntity alarmConfig) {
        List<Map<String, Object>> bindings = new ArrayList<>();

        bindings.addAll((List) data.getOutput().getOrDefault("_bindings", Collections.emptyList()));

        //添加告警配置创建人到bindings中。作为用户维度信息
        Map<String, Object> userDimension = new HashMap<>(2);
        userDimension.put("type", "user");
        userDimension.put("id", alarmConfig.getCreatorId());
        bindings.add(userDimension);
        //添加组织纬度信息
        if ("org".equals(alarmConfig.getTargetType())) {
            Map<String, Object> orgDimension = new HashMap<>(2);
            userDimension.put("type", targetInfo.getTargetType());
            userDimension.put("id", targetInfo.getTargetId());
            bindings.add(userDimension);
        }
        return bindings;
    }

}
