package org.jetlinks.community.command.rule.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 告警结果
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AlarmResult implements Serializable {

    private static final long serialVersionUID = -1752497262936740164L;

    @Schema(description = "告警ID")
    private String recordId;

    @Schema(description = "是否重复告警")
    private boolean alarming;

    @Schema(description = "当前首次触发")
    private boolean firstAlarm;

    @Schema(description = "上一次告警时间")
    private long lastAlarmTime;

    @Schema(description = "首次告警或者解除告警后的再一次告警时间")
    private long alarmTime;
}