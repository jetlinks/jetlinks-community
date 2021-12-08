package org.jetlinks.community.device.measurements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceObjectDefinitionTest {

  @Test
  void getId() {
      String id = DeviceObjectDefinition.message.getId();
      assertNotNull(id);
      String name = DeviceObjectDefinition.message.getName();
      assertNotNull(name);
  }
}