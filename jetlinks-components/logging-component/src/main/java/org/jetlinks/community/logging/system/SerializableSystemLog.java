package org.jetlinks.community.logging.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SerializableSystemLog implements Serializable {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "模块")
    private String mavenModule;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "线程名")
    private String threadName;

    @Schema(description = "日志级别")
    private String level;

    @Schema(description = "类名")
    private String className;

    @Schema(description = "方法名")
    private String methodName;

    @Schema(description = "行号")
    private int lineNumber;

    @Schema(description = "代码地址")
    private String java;

    @Schema(description = "日志内容")
    private String message;

    @Schema(description = "异常栈")
    private String exceptionStack;

    @Schema(description = "日志时间")
    private long createTime;

    @Schema(description = "线程ID")
    private String threadId;

    @Schema(description = "上下文")
    private Map<String, String> context;
}
