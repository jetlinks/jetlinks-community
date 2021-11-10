package org.jetlinks.community.notify.manager.web;

import org.jetlinks.community.notify.manager.service.NotifyHistoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotifierHistoryControllerTest {

  @Test
  void getService() {
      new NotifierHistoryController(new NotifyHistoryService()).getService();
  }
}