package org.jetlinks.community.device.web.protocol;

import org.jetlinks.core.message.codec.DefaultTransport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransportInfoTest {

  @Test
  void of() {
      TransportInfo info = TransportInfo.of(DefaultTransport.CoAP);
      assertNotNull(info.getId());
      assertNotNull(info.getName());
      TransportInfo transportInfo = new TransportInfo();
      transportInfo.setId("id");
      transportInfo.setName("name");
      assertNotNull(transportInfo.getId());
      assertNotNull(transportInfo.getName());
  }
}