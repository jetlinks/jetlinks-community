package org.jetlinks.community.rule.engine.web;

import org.jetlinks.community.rule.engine.service.DeviceAlarmHistoryService;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

@WebFluxTest(DeviceAlarmHistoryController.class)
class DeviceAlarmHistoryControllerTest extends TestJetLinksController {
    private static final String BASE_URL="/device/alarm/history";
    private static final String ID="test";
    @Test
    void getService() {
        new DeviceAlarmHistoryController(new DeviceAlarmHistoryService()).getService();
    }

    @Test
    void changeState() {
        client.put()
            .uri(BASE_URL+"/"+ID+"/_newer")
            .bodyValue("test")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}