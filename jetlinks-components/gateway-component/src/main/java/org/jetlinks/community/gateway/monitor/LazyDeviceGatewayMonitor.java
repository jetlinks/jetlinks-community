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
package org.jetlinks.community.gateway.monitor;

import java.util.function.Supplier;

class LazyDeviceGatewayMonitor implements DeviceGatewayMonitor {

    private volatile DeviceGatewayMonitor target;

    private Supplier<DeviceGatewayMonitor> monitorSupplier;

    public LazyDeviceGatewayMonitor(Supplier<DeviceGatewayMonitor> monitorSupplier) {
        this.monitorSupplier = monitorSupplier;
    }

    public DeviceGatewayMonitor getTarget() {
        if (target == null) {
            target = monitorSupplier.get();
        }
        return target;
    }


    @Override
    public void totalConnection(long total) {
        getTarget().totalConnection(total);
    }

    @Override
    public void connected() {
        getTarget().connected();
    }

    @Override
    public void rejected() {
        getTarget().rejected();
    }

    @Override
    public void disconnected() {
        getTarget().disconnected();
    }

    @Override
    public void receivedMessage() {
        getTarget().receivedMessage();
    }

    @Override
    public void sentMessage() {
        getTarget().sentMessage();
    }
}
