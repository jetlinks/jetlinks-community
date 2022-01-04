package org.jetlinks.community.device.message.writer;

import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TimeSeriesMessageWriterConnectorTest {

  @Test
  void writeDeviceMessageToTs() {
      DeviceDataService dataService = Mockito.mock(DeviceDataService.class);
      TimeSeriesMessageWriterConnector connector = new TimeSeriesMessageWriterConnector(dataService);

      EventMessage eventMessage = new EventMessage();

      Mockito.when(dataService.saveDeviceMessage(Mockito.any(DeviceMessage.class)))
          .thenReturn(Mono.just(1).then());
      assertNotNull(connector);
      connector.writeDeviceMessageToTs(eventMessage).subscribe();
  }
}