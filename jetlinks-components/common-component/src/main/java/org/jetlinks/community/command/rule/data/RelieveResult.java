package org.jetlinks.community.command.rule.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 解除警告结果
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelieveResult extends AlarmResult{

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "告警原因描述")
    private String actualDesc;

    @Schema(description = "解除原因")
    private String relieveReason;

    @Schema(description = "解除时间")
    private long relieveTime;

    @Schema(description = "解除说明")
    private String describe;
}