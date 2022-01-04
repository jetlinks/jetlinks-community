package org.jetlinks.community.notify.manager.web;

import org.jetlinks.community.notify.manager.service.NotifyHistoryService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifierHistoryControllerTest {

  @Test
  void getService() {
      NotifyHistoryService service = new NotifierHistoryController(new NotifyHistoryService()).getService();
      assertNotNull(service);
  }
}