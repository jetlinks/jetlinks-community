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
package org.jetlinks.community.gateway.monitor.measurements;

import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.springframework.stereotype.Component;

import static org.jetlinks.community.dashboard.MeasurementDefinition.of;

@Component
public class DeviceGatewayMeasurementProvider extends StaticMeasurementProvider {

    public DeviceGatewayMeasurementProvider(TimeSeriesManager timeSeriesManager) {
        super(GatewayDashboardDefinition.gatewayMonitor, GatewayObjectDefinition.deviceGateway);

        addMeasurement(new DeviceGatewayMeasurement(of("connection", "连接数"), "value", Aggregation.MAX, timeSeriesManager));

        addMeasurement(new DeviceGatewayMeasurement(of("connected", "创建连接数"), "count", Aggregation.SUM, timeSeriesManager));
        addMeasurement(new DeviceGatewayMeasurement(of("rejected", "拒绝连接数"), "count", Aggregation.SUM, timeSeriesManager));
        addMeasurement(new DeviceGatewayMeasurement(of("disconnected", "断开连接数"), "count", Aggregation.SUM, timeSeriesManager));
        addMeasurement(new DeviceGatewayMeasurement(of("received_message", "接收消息数"), "count", Aggregation.SUM, timeSeriesManager));
        addMeasurement(new DeviceGatewayMeasurement(of("sent_message", "发送消息数"), "count", Aggregation.SUM, timeSeriesManager));
    }
}
