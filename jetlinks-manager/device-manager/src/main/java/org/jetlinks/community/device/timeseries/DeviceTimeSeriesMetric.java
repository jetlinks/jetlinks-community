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
package org.jetlinks.community.device.timeseries;

import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

/**
 * 设备时序数据度量标识
 *
 * @author zhouhao
 * @see org.jetlinks.community.timeseries.TimeSeriesService
 * @see TimeSeriesMetric
 * @see DeviceTimeSeriesMetadata
 * @since 1.0
 */
public interface DeviceTimeSeriesMetric {

    /**
     * 获取指定设备型号和事件的度量标识
     *
     * @param productId 型号ID {@link DeviceProductOperator#getId()}
     * @param eventId   事件ID {@link EventMetadata#getId()}
     * @return 度量标识
     */
    static TimeSeriesMetric deviceEventMetric(String productId, String eventId) {
        return TimeSeriesMetric.of(deviceEventMetricId(productId, eventId));
    }

    static String deviceEventMetricId(String productId, String eventId) {
        return "event_".concat(productId).concat("_").concat(eventId);
    }

    /**
     * 获取指定设备型号的设备属性度量标识
     *
     * @param productId 型号ID
     * @return 度量标识
     */
    static TimeSeriesMetric devicePropertyMetric(String productId) {
        return TimeSeriesMetric.of(devicePropertyMetricId(productId));
    }

    static String devicePropertyMetricId(String productId) {
        return "properties_".concat(productId);
    }

    /**
     * 获取指定设备型号的设备操作日志度量标识
     *
     * @param productId 设备型号ID
     * @return 度量标识
     */
    static TimeSeriesMetric deviceLogMetric(String productId) {
        return TimeSeriesMetric.of(deviceLogMetricId(productId));
    }

    static String deviceLogMetricId(String productId) {
        return "device_log_".concat(productId);
    }

    /**
     * 设备消息监控指标,用于对设备消息相关进行监控
     *
     * @return 度量标识
     */
    static TimeSeriesMetric deviceMetrics() {
        return TimeSeriesMetric.of("device_metrics");
    }
}
