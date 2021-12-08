package org.jetlinks.community.network.manager.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugUtilsTest {

  @Test
  void stringToBytes() {
      byte[] bytes = DebugUtils.stringToBytes("");
      assertNotNull(bytes);
      byte[] bytes1 = DebugUtils.stringToBytes("0x1111");
      assertNotNull(bytes1);
      byte[] bytes2 = DebugUtils.stringToBytes("123");
      assertNotNull(bytes2);
  }
}