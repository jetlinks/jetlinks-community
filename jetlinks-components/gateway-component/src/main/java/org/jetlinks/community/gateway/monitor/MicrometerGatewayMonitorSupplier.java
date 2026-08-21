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

import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.springframework.stereotype.Component;

/**
 * 基于 Micrometer 的设备网关监控供应商。
 *
 * <p>由 Spring 创建后注册到 {@link GatewayMonitors}，并为每个网关创建独立监控实例。</p>
 *
 * @see MicrometerDeviceGatewayMonitor
 * @since 1.0
 */
@Component
public class MicrometerGatewayMonitorSupplier implements DeviceGatewayMonitorSupplier {

    private final MeterRegistryManager meterRegistryManager;

    public MicrometerGatewayMonitorSupplier(MeterRegistryManager meterRegistryManager) {
        this.meterRegistryManager = meterRegistryManager;
        GatewayMonitors.register(this);
    }

    @Override
    public DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags) {
        return new MicrometerDeviceGatewayMonitor(meterRegistryManager.getMeterRegister(GatewayTimeSeriesMetric.deviceGatewayMetric, "target"), id, tags);
    }
}
