package org.jetlinks.community.device.measurements.message;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;


class DeviceMessageMeasurementProviderTest {

    @Test
    void incrementMessage() {

        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        MeterRegistryManager meterRegistryManager = Mockito.mock(MeterRegistryManager.class);
        MeterRegistry meterRegistry = Mockito.mock(MeterRegistry.class);
        Counter counter = Mockito.mock(Counter.class);
        Mockito.when(
            meterRegistryManager.getMeterRegister(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())
        ).thenReturn(meterRegistry);

        Mockito.when(
            meterRegistry.counter(Mockito.anyString(),Mockito.any(String[].class))
        ).thenReturn(counter);

        Mockito.when(
            meterRegistry.counter(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())
        ).thenReturn(counter);

        DeviceMessageMeasurementProvider provider = new DeviceMessageMeasurementProvider(new BrokerEventBus(),meterRegistryManager,registry,timeSeriesManager);

        provider.incrementMessage(null)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

        provider.incrementMessage(new EventMessage())
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();

    }
}