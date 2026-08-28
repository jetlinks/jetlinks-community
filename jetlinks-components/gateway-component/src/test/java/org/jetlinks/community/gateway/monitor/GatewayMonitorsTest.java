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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.micrometer.MeterRegistrySupplier;
import org.jetlinks.core.metadata.DataType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class GatewayMonitorsTest {

    @Test
    void shouldUseNoopMonitorUntilSupplierIsRegistered() {
        DeviceGatewayMonitor monitor = GatewayMonitors.getDeviceGatewayMonitor("test");

        assertSame(GatewayMonitors.nonDevice, ((LazyDeviceGatewayMonitor) monitor).getTarget());
        execute(monitor);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterRegistryManager manager = new MeterRegistryManager(
            Arrays.asList(new TestMeterRegistrySupplier(registry)),
            null
        );

        new MicrometerGatewayMonitorSupplier(manager);

        monitor = GatewayMonitors.getDeviceGatewayMonitor("test");
        assertNotSame(GatewayMonitors.nonDevice, ((LazyDeviceGatewayMonitor) monitor).getTarget());

        monitor.totalConnection(2);
        monitor.connected();
        monitor.rejected();
        monitor.disconnected();
        monitor.receivedMessage();
        monitor.sentMessage();

        assertEquals(2, registry.get("test")
            .tag("target", "connection")
            .gauge()
            .value());
        assertEquals(1, registry.get("test")
            .tag("target", "connected")
            .counter()
            .count());
        assertEquals(1, registry.get("test")
            .tag("target", "rejected")
            .counter()
            .count());
        assertEquals(1, registry.get("test")
            .tag("target", "disconnected")
            .counter()
            .count());
        assertEquals(1, registry.get("test")
            .tag("target", "received_message")
            .counter()
            .count());
        assertEquals(1, registry.get("test")
            .tag("target", "sent_message")
            .counter()
            .count());
    }

    private void execute(DeviceGatewayMonitor monitor) {
        monitor.connected();
        monitor.totalConnection(1);
        monitor.rejected();
        monitor.disconnected();
        monitor.receivedMessage();
        monitor.sentMessage();
    }

    private static class TestMeterRegistrySupplier implements MeterRegistrySupplier {

        private final MeterRegistry registry;

        private TestMeterRegistrySupplier(MeterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public MeterRegistry getMeterRegistry(String metric, String... tagKeys) {
            return registry;
        }

        @Override
        public MeterRegistry getMeterRegistry(String metric, Map<String, DataType> tagDefine) {
            return registry;
        }
    }
}
