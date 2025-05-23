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
