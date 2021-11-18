package org.jetlinks.community.device.response;

import org.jetlinks.community.device.enums.DeviceState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRunInfoTest {

  @Test
  void get() {
      DeviceRunInfo of = DeviceRunInfo.of(10l, 10l, DeviceState.online, "test", "test");

      of.setProductId("test");
      String metadata = of.getMetadata();
      assertNotNull(metadata);
      String productId = of.getProductId();
      assertNotNull(productId);
      DeviceRunInfo.builder().productId("test").build();

      ImportDeviceInstanceResult.error("test");
      ImportDeviceInstanceResult.error(new Throwable("ttt"));

  }
}