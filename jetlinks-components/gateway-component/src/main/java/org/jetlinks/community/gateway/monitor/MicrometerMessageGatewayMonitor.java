package org.jetlinks.community.gateway.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
class MicrometerMessageGatewayMonitor implements MessageGatewayMonitor {
    MeterRegistry registry;

    String id;
    String[] tags;
    private final AtomicReference<Long> totalRef = new AtomicReference<>(0L);

    public MicrometerMessageGatewayMonitor(MeterRegistry registry, String id, String[] tags) {
        this.registry = registry;
        this.id = id;
        this.tags = tags;
        Gauge
            .builder(id, totalRef, AtomicReference::get)
            .tags(tags)
            .tag("target", "sessionNumber")
            .register(registry);

        this.acceptedSession = getCounter("accepted_session");
        this.closedSession = getCounter("closed_session");
        this.subscribed = getCounter("subscribed");
        this.unsubscribed = getCounter("unsubscribed");
        this.acceptMessage = getCounter("accept_message");

    }


    @Override
    public void totalSession(long sessionNumber) {
        totalRef.set(Math.max(0, sessionNumber));
    }

    final Counter acceptedSession;
    final Counter closedSession;
    final Counter subscribed;
    final Counter unsubscribed;
    final Counter acceptMessage;

    private Counter getCounter(String target) {
        return Counter
            .builder(id)
            .tags(tags)
            .tag("target", target)
            .register(registry);
    }

    @Override
    public void acceptedSession() {
        acceptedSession
            .increment();
    }

    @Override
    public void closedSession() {
        closedSession
            .increment();
    }

    @Override
    public void subscribed() {
        subscribed
            .increment();
    }

    @Override
    public void unsubscribed() {
        unsubscribed
            .increment();
    }

    @Override
    public void dispatched(String connector) {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "dispatched")
            .tag("connector", connector)
            .register(registry)
            .increment();
    }

    @Override
    public void acceptMessage() {
        acceptedSession
            .increment();
    }

    @Override
    public void dispatchError(String connector, String sessionId, Throwable error) {
        Counter
            .builder(id)
            .tags(tags)
            .tag("connector", connector)
            .tag("target", "error")
            .tag("message", error.getMessage())
            .tag("errorType", error.getClass().getSimpleName())
            .register(registry)
            .increment();
    }
}
