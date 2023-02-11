package org.jetlinks.community.dashboard.measurements.sys;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@ToString
public class SystemInfo implements MonitorInfo<SystemInfo> {

    private CpuInfo cpu;
    private MemoryInfo memory;
    private DiskInfo disk;

    @Override
    public SystemInfo add(SystemInfo info) {
        return new SystemInfo(
            this.cpu.add(info.cpu),
            this.memory.add(info.memory),
            this.disk.add(info.disk)
        );
    }

    @Override
    public SystemInfo division(int num) {

        return new SystemInfo(
            this.cpu.division(num),
            this.memory.division(num),
            this.disk.division(num)
        );
    }
}
