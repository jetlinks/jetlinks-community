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
package org.jetlinks.community.timescaledb.timeseries;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.Interval;
import org.jetlinks.community.timescaledb.TimescaleDBUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ConfigurationProperties(prefix = "timescaledb.time-series")
public class TimescaleDBTimeSeriesProperties {
    private boolean enabled = true;

    /**
     * 分区时间间隔
     */
    private Interval chunkTimeInterval = Interval.ofDays(7);

    /**
     * 对特定的表设置数据保留时长
     */
    private List<RetentionPolicy> retentionPolicies = new ArrayList<>();

    /**
     * 默认数据保留时长
     */
    private Interval retentionPolicy;


    public Interval getRetentionPolicy(String tableName) {

        for (RetentionPolicy policy : retentionPolicies) {
            if (Objects.equals(TimescaleDBUtils.getTableName(policy.table), tableName)) {
                return policy.interval;
            }
        }
        return retentionPolicy;
    }

    @Getter
    @Setter
    public static class RetentionPolicy {
        private String table;
        private Interval interval;
    }
}
