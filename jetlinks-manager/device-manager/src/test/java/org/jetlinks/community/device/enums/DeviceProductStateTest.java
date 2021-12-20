package org.jetlinks.community.device.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceProductStateTest {

  @Test
  void getName() {
      String name = DeviceProductState.unregistered.getName();
      assertNotNull(name);
      String text = DeviceProductState.unregistered.getText();
      assertNotNull(text);
      String name1 = DeviceProductState.of((byte) 0).getName();
      assertNotNull(name1);
  }
}