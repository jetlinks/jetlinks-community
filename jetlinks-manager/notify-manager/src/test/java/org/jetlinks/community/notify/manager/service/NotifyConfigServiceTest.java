package org.jetlinks.community.notify.manager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifyConfigServiceTest {

  @Test
  void getCacheName() {
      NotifyConfigService notifyConfigService = new NotifyConfigService();
      String cacheName = notifyConfigService.getCacheName();
      assertNotNull(cacheName);
      assertEquals("notify_config",cacheName);
  }
}