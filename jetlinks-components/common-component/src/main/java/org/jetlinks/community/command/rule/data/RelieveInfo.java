package org.jetlinks.community.command.rule.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 解除告警参数
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelieveInfo extends AlarmInfo{

    @Schema(description = "解除原因")
    private String relieveReason;

    @Schema(description = "解除说明")
    private String describe;

    /**
     * 告警解除类型，人工（user）、系统（system）等
     */
    @Schema(description = "告警解除类型")
    private String alarmRelieveType;
}