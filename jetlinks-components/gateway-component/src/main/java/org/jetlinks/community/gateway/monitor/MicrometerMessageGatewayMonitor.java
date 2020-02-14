package org.jetlinks.community.gateway.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class MicrometerMessageGatewayMonitor implements MessageGatewayMonitor {
    MeterRegistry registry;

    String id;
    String[] tags;

    @Override
    public void totalSession(long sessionNumber) {
        Gauge
            .builder(id, sessionNumber, Number::doubleValue)
            .tags(tags)
            .tag("target", "sessionNumber")
            .register(registry);
    }

    @Override
    public void acceptedSession() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "acceptedSession")
            .register(registry)
            .increment();
    }

    @Override
    public void closedSession() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "closedSession")
            .register(registry)
            .increment();
    }

    @Override
    public void subscribed() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "subscribed")
            .register(registry)
            .increment();
    }

    @Override
    public void unsubscribed() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "unsubscribed")
            .register(registry)
            .increment();
    }

    @Override
    public void dispatched(String connector) {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "dispatched")
            .tag("connector",connector)
            .register(registry)
            .increment();
    }

    @Override
    public void acceptMessage() {
        Counter
            .builder(id)
            .tags(tags)
            .tag("target", "acceptMessage")
            .register(registry)
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
