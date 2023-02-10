package org.jetlinks.community.dashboard.measurements.sys;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DiskInfo implements MonitorInfo<DiskInfo> {
    private static final long serialVersionUID = 1L;

    @Schema(description = "磁盘总容量,单位MB")
    private long total;

    @Schema(description = "磁盘可用容量,单位MB")
    private long free;

    public float getUsage() {
        return MonitorUtils.round(
            ((total - free) / (float) total) * 100
        );
    }

    @Override
    public DiskInfo add(DiskInfo info) {
        return new DiskInfo(
            info.total + this.total,
            info.free + this.free
        );
    }

    @Override
    public DiskInfo division(int num) {
        return new DiskInfo(
            this.total / num,
            this.free / num
        );
    }
}
