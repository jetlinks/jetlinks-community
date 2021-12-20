package org.jetlinks.community.device.web;

import org.jetlinks.community.device.entity.DeviceCategory;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(DeviceCategoryController.class)
class DeviceCategoryControllerTest extends TestJetLinksController {

    public static final String BASE_URL = "/device/category";

    @Test
    void getAllCategory() {
        List<DeviceCategory> responseBody = client.get()
            .uri(BASE_URL)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceCategory.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("智能城市", responseBody.get(0).getName());
    }

    @Test
    void getAllCategory2() {
        List<DeviceCategory> responseBody = client.get()
            .uri(BASE_URL + "/_query/no-paging")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceCategory.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("智能城市", responseBody.get(0).getName());
    }

    @Test
    void getAllCategoryTree() {
        List<DeviceCategory> responseBody = client.get()
            .uri(BASE_URL + "/_tree")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DeviceCategory.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("智能城市", responseBody.get(0).getName());
    }
}