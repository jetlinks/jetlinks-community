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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.atomic.AtomicReference;

class MicrometerDeviceGatewayMonitor implements DeviceGatewayMonitor {
    MeterRegistry registry;
    String id;
    String[] tags;

    private AtomicReference<Long> totalRef = new AtomicReference<>(0L);

    public MicrometerDeviceGatewayMonitor(MeterRegistry registry, String id, String[] tags) {
        this.registry = registry;
        this.id = id;
        this.tags = tags;
        Gauge
            .builder(id, totalRef, AtomicReference::get)
            .tags(tags)
            .tag("target", "connection")
            .register(registry);

        this.connected = getCounter("connected");
        this.rejected = getCounter("rejected");
        this.disconnected = getCounter("disconnected");
        this.sentMessage = getCounter("sent_message");
        this.receivedMessage = getCounter("received_message");
    }

    final Counter connected;
    final Counter rejected;
    final Counter disconnected;
    final Counter receivedMessage;
    final Counter sentMessage;


    private Counter getCounter(String target) {
        return Counter
            .builder(id)
            .tags(tags)
            .tag("target", target)
            .register(registry);
    }

    @Override
    public void totalConnection(long total) {
        totalRef.set(Math.max(0, total));
    }

    @Override
    public void connected() {
        connected.increment();
    }

    @Override
    public void rejected() {
        rejected.increment();
    }

    @Override
    public void disconnected() {
        disconnected.increment();
    }

    @Override
    public void receivedMessage() {
        receivedMessage.increment();
    }

    @Override
    public void sentMessage() {
        sentMessage.increment();
    }
}
