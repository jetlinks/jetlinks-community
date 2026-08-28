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

/**
 * 设备网关监控供应商。
 *
 * <p>通过 {@link GatewayMonitors#register(DeviceGatewayMonitorSupplier)} 注册后，
 * 在设备网关首次使用监控能力时按网关标识创建监控实例。</p>
 *
 * @see DeviceGatewayMonitor
 * @see GatewayMonitors
 * @since 1.0
 */
public interface DeviceGatewayMonitorSupplier {

    /**
     * 为指定设备网关创建监控实例。
     *
     * <p>该方法可能由多个网关并发调用。返回 {@code null} 表示当前供应商不监控该网关；
     * 返回的监控实例应遵循 {@link DeviceGatewayMonitor} 的非阻塞与响应式包装约束。</p>
     *
     * @param id   设备网关标识
     * @param tags 网关附加标签
     * @return 监控实例，或 {@code null} 跳过当前供应商
     */
    DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags);

}
