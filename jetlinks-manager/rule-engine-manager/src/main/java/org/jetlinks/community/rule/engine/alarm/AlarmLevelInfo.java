package org.jetlinks.community.rule.engine.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmLevelInfo {

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "名称")
    private String title;
}
