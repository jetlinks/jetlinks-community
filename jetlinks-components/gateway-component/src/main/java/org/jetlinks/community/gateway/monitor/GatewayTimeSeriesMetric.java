/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.gateway.monitor;

import org.jetlinks.community.timeseries.TimeSeriesMetric;

/**
 * 网关监控使用的时序指标定义。
 *
 * <p>统一设备网关监控数据的时序存储名称，不负责指标采集和查询。</p>
 *
 * @see DeviceGatewayMonitor
 * @since 1.0
 */
public interface GatewayTimeSeriesMetric {

    String deviceGatewayMetric = "device_gateway_monitor";

    /**
     * @return 网关设备监控指标
     * @see DeviceGatewayMonitor
     */
    static TimeSeriesMetric deviceGatewayMetric() {
        return TimeSeriesMetric.of(deviceGatewayMetric);
    }
}
