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
