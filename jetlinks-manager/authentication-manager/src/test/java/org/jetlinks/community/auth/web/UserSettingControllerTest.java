package org.jetlinks.community.auth.web;

import org.jetlinks.community.auth.entity.UserSettingEntity;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebFluxTest(UserSettingController.class)
class UserSettingControllerTest extends TestJetLinksController {


    @Test
    void testCrud() {
        String type = "user-search";

        UserSettingEntity newSetting = new UserSettingEntity();
        newSetting.setName("test");
        newSetting.setContent("test-content");
        client
            .post()
            .uri("/user/settings/{type}", type)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(newSetting)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        List<UserSettingEntity> entities = client
            .get()
            .uri("/user/settings/{type}", type)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(UserSettingEntity.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(entities);
        assertEquals(1, entities.size());

        UserSettingEntity entity = client
            .get()
            .uri("/user/settings/{type}/{key}", type, entities.get(0).getKey())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(UserSettingEntity.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(entity);
        assertEquals(entity.getContent(), newSetting.getContent());

        client
            .delete()
            .uri("/user/settings/{type}/{key}", type, entity.getKey())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


        UserSettingEntity entity1 = client
            .get()
            .uri("/user/settings/{type}/{key}", type, entity.getKey())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(UserSettingEntity.class)
            .returnResult()
            .getResponseBody();
        assertNull(entity1);


    }

}