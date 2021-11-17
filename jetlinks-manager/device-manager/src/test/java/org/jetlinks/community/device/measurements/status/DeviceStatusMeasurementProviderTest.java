package org.jetlinks.community.device.measurements.status;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class DeviceStatusMeasurementProviderTest {

  @Test
  void incrementOnline() {
      DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
      TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
      MeterRegistryManager meterRegistryManager = Mockito.mock(MeterRegistryManager.class);
      LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
      MeterRegistry meterRegistry = Mockito.mock(MeterRegistry.class);
      Counter counter = Mockito.mock(Counter.class);
      Mockito.when(
          meterRegistryManager.getMeterRegister(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())
      ).thenReturn(meterRegistry);

      Mockito.when(
          meterRegistry.counter(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())
      ).thenReturn(counter);

      DeviceStatusMeasurementProvider provider = new DeviceStatusMeasurementProvider(meterRegistryManager,instanceService,timeSeriesManager,new BrokerEventBus());

      EventMessage eventMessage = new EventMessage();
      eventMessage.addHeader("productId","productId");
      provider.incrementOnline(eventMessage)
          .as(StepVerifier::create)
          .expectSubscription()
          .verifyComplete();

      provider.incrementOffline(eventMessage)
          .as(StepVerifier::create)
          .expectSubscription()
          .verifyComplete();
  }

  @Test
  void incrementOffline() {

  }
}