package org.jetlinks.community.dashboard.measurements.sys;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CpuInfo implements MonitorInfo<CpuInfo> {
    private static final long serialVersionUID = 1L;

    @Schema(description = "JVM进程CPU使用率,0-100")
    private float jvmUsage;

    @Schema(description = "系统CPU使用率,0-100")
    private float systemUsage;

    @Override
    public CpuInfo add(CpuInfo info) {
        return new CpuInfo(
            MonitorUtils.round(info.jvmUsage + this.jvmUsage),
            MonitorUtils.round(info.systemUsage + this.systemUsage)
        );
    }

    @Override
    public CpuInfo division(int num) {
        return new CpuInfo(
            MonitorUtils.round(this.jvmUsage / num),
            MonitorUtils.round(this.systemUsage / num)
        );
    }

}
