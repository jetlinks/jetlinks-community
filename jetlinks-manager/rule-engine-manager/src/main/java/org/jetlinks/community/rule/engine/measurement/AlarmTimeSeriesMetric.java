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
package org.jetlinks.community.rule.engine.measurement;

import org.jetlinks.community.timeseries.TimeSeriesMetric;

/**
 * 媒体时序数据度量标识
 *
 * @author bestfeng
 *
 * @see org.jetlinks.community.timeseries.TimeSeriesService
 * @see TimeSeriesMetric
 */
public interface AlarmTimeSeriesMetric {

    /**
     * 告警监控指标,用于对告警进行进行监控
     *
     * @return 度量标识
     */
    static TimeSeriesMetric alarmStreamMetrics() {
        return TimeSeriesMetric.of("alarm_history");
    }
}
