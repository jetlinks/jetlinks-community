package org.jetlinks.community.dashboard.measurements.sys;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MemoryInfo implements MonitorInfo<MemoryInfo> {
    private static final long serialVersionUID = 1L;

    @Schema(description = "JVM堆总内存,单位MB")
    private long jvmHeapTotal;

    @Schema(description = "JVM堆可用内存,单位MB")
    private long jvmHeapFree;

    @Schema(description = "JVM堆外总内存,单位MB")
    private long jvmNonHeapTotal;

    @Schema(description = "JVM堆外可用内存,单位MB")
    private long jvmNonHeapFree;

    @Schema(description = "系统总内存,单位MB")
    private long systemTotal;

    @Schema(description = "系统可用内存,单位MB")
    private long systemFree;

    public float getJvmHeapUsage() {
        return MonitorUtils.round(
            ((jvmHeapTotal - jvmHeapFree) / (float) jvmHeapTotal)*100
        );
    }

    public float getJvmNonHeapUsage() {
        return MonitorUtils.round(
            ((jvmNonHeapTotal - jvmNonHeapFree) / (float) jvmNonHeapTotal)*100
        );
    }

    public float getSystemUsage() {
        return MonitorUtils.round(
            ((systemTotal - systemFree) / (float) systemTotal)*100
        );
    }

    @Override
    public MemoryInfo add(MemoryInfo info) {
        return new MemoryInfo(
            info.jvmHeapTotal + this.jvmHeapTotal,
            info.jvmHeapFree + this.jvmHeapFree,
            info.jvmNonHeapTotal + this.jvmNonHeapTotal,
            info.jvmNonHeapFree + this.jvmNonHeapFree,
            info.systemTotal + this.systemTotal,
            info.systemFree + this.systemFree
        );
    }

    @Override
    public MemoryInfo division(int num) {
        return new MemoryInfo(
            this.jvmHeapTotal / num,
            this.jvmHeapFree / num,
            this.jvmNonHeapTotal / num,
            this.jvmNonHeapFree / num,
            this.systemTotal / num,
            this.systemFree / num
        );
    }
}
