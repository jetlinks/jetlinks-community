package org.jetlinks.community.rule.engine.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.enums.AlarmHandleType;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmHandleInfo {

    @Schema(description = "告警记录ID")
    @NotBlank
    private String alarmRecordId;

    @Schema(description = "告警ID")
    @NotBlank
    private String alarmConfigId;

    @Schema(description = "告警时间")
    @NotNull
    private Long alarmTime;

    @Schema(description = "处理说明")
    private String describe;

    @Schema(description = "处理时间")
    private Long handleTime;

    @Schema(description = "处理类型")
    private String type;

    @Schema(description = "处理后的状态")
    @NotBlank
    private AlarmRecordState state;


}
