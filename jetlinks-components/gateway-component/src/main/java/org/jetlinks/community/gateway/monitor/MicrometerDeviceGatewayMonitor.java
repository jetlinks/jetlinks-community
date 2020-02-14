package org.jetlinks.community.gateway.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class MicrometerDeviceGatewayMonitor implements DeviceGatewayMonitor {
    MeterRegistry registry;

    String id;
    String[] tags;

    @Override
    public void totalConnection(long total) {
        Gauge
            .builder(id, total, Number::doubleValue)
            .tags(tags)
            .tag("target", "connection")
            .register(registry);
    }

    @Override
    public void connected() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "connected")
            .register(registry)
            .increment();
    }

    @Override
    public void rejected() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "rejected")
            .register(registry)
            .increment();
    }

    @Override
    public void disconnected() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "disconnected")
            .register(registry)
            .increment();
    }

    @Override
    public void receivedMessage() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "receivedMessage")
            .register(registry)
            .increment();
    }

    @Override
    public void sentMessage() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "sentMessage")
            .register(registry)
            .increment();
    }
}
