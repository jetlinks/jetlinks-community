package org.jetlinks.community.command.rule.data;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.terms.TermSpec;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 触发告警参数
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlarmInfo implements Serializable {
    private static final long serialVersionUID = -2316376361116648370L;

    @Schema(description = "告警配置ID")
    private String alarmConfigId;

    @Schema(description = "告警名称")
    private String alarmName;

    @Schema(description = "告警说明")
    private String description;

    @Schema(description = "告警级别")
    private int level;

    @Schema(description = "告警目标类型")
    private String targetType;

    @Schema(description = "告警目标ID")
    private String targetId;

    @Schema(description = "告警目标名称")
    private String targetName;

    @Schema(description = "告警来源类型")
    private String sourceType;

    @Schema(description = "告警来源ID")
    private String sourceId;

    @Schema(description = "告警来源名称")
    private String sourceName;

    /**
     * 标识告警触发的配置来自什么业务功能
     */
    @Schema(description = "告警配置源")
    private String alarmConfigSource;

    @Schema(description = "告警数据")
    private Map<String, Object> data;

    /**
     * 告警触发条件
     */
    private TermSpec termSpec;
}