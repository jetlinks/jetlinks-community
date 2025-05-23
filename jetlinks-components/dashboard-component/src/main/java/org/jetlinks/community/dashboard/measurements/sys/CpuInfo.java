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

    public CpuInfo max(CpuInfo cpu) {
        return new CpuInfo(
            Math.max(this.jvmUsage, cpu.jvmUsage),
            Math.max(this.systemUsage, cpu.systemUsage)
        );
    }

}
