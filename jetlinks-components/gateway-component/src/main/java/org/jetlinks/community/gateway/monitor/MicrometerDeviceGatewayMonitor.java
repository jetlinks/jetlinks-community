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
