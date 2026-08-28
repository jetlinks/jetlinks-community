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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 设备网关监控注册与获取入口。
 *
 * <p>供应商使用写时复制集合保存，支持在网关运行期间注册。返回的监控会延迟解析供应商，
 * 以便网关可以早于监控组件创建。</p>
 *
 * @see DeviceGatewayMonitor
 * @see DeviceGatewayMonitorSupplier
 * @since 1.0
 */
public class GatewayMonitors {

    private static final List<DeviceGatewayMonitorSupplier> deviceGatewayMonitorSuppliers = new CopyOnWriteArrayList<>();

    /**
     * 未注册有效供应商时使用的空监控。
     *
     * @since 2.12
     */
    public static final DeviceGatewayMonitor nonDevice = new NoneDeviceGatewayMonitor();

    /**
     * 注册设备网关监控供应商。
     *
     * @param supplier 监控供应商
     */
    public static void register(DeviceGatewayMonitorSupplier supplier) {
        deviceGatewayMonitorSuppliers.add(supplier);
    }

    private static DeviceGatewayMonitor doGetDeviceGatewayMonitor(String id, String... tags) {
        List<DeviceGatewayMonitor> all = deviceGatewayMonitorSuppliers.stream()
            .map(supplier -> supplier.getDeviceGatewayMonitor(id, tags))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (all.isEmpty()) {
            return nonDevice;
        }
        if (all.size() == 1) {
            return all.get(0);
        }
        CompositeDeviceGatewayMonitor monitor = new CompositeDeviceGatewayMonitor();
        monitor.add(all);
        return monitor;
    }

    /**
     * 获取指定设备网关的延迟监控实例。
     *
     * <p>首次调用监控 API 时才解析已注册供应商。多个供应商返回监控时，
     * 将按注册顺序组合执行。</p>
     *
     * @param id   设备网关标识
     * @param tags 网关附加标签
     * @return 延迟解析的设备网关监控
     */
    public static DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags) {
        return new LazyDeviceGatewayMonitor(() -> doGetDeviceGatewayMonitor(id, tags));
    }
}
