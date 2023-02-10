package org.jetlinks.community.dashboard.measurements.sys;

import reactor.core.publisher.Mono;

public interface SystemMonitorService {

    Mono<SystemInfo> system();

    Mono<MemoryInfo> memory();

    Mono<CpuInfo> cpu();

    Mono<DiskInfo> disk();
}
