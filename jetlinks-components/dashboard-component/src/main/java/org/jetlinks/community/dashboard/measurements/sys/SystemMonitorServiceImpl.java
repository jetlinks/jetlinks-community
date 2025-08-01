/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.dashboard.measurements.sys;

import org.jetlinks.community.dashboard.measurements.SystemMonitor;
import reactor.core.publisher.Mono;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class SystemMonitorServiceImpl implements SystemMonitorService {

    private final static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private final static int MB = 1024 * 1024;

    @Override
    public Mono<SystemInfo> system() {
        return Mono
            .zip(
                cpu(),
                memory(),
                disk()
            )
            .map(tp3 -> SystemInfo.of(tp3.getT1(), tp3.getT2(), tp3.getT3()));
    }

    @Override
    public Mono<DiskInfo> disk() {
        long total = 0, usable = 0;
        for (File file : File.listRoots()) {
            total += file.getTotalSpace();
            usable += file.getUsableSpace();
        }
        DiskInfo diskInfo = new DiskInfo();
        diskInfo.setTotal(total / MB);
        diskInfo.setFree(usable / MB);
        return Mono.just(diskInfo);
    }

    public Mono<MemoryInfo> memory() {
        MemoryInfo info = new MemoryInfo();
        info.setSystemTotal((long) SystemMonitor.totalSystemMemory.value());
        info.setSystemFree((long) SystemMonitor.freeSystemMemory.value());

        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        long nonHeapMax = (nonHeap.getMax() > 0 ? nonHeap.getMax() / MB : info.getSystemTotal());

        info.setJvmHeapFree((heap.getMax() - heap.getUsed()) / MB);
        info.setJvmHeapTotal(heap.getMax() / MB);

        info.setJvmNonHeapFree(nonHeapMax - nonHeap.getUsed() / MB);
        info.setJvmNonHeapTotal(nonHeapMax);
        return Mono.just(info);
    }

    public Mono<CpuInfo> cpu() {
        CpuInfo info = new CpuInfo();

        info.setSystemUsage(MonitorUtils.round((float) (SystemMonitor.systemCpuUsage.value())));
        info.setJvmUsage(MonitorUtils.round((float) (SystemMonitor.jvmCpuUsage.value())));

        return Mono.just(info);
    }
}
