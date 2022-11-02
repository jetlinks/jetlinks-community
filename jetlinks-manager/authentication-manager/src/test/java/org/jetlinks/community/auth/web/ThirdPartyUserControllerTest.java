package org.jetlinks.community.auth.web;

import org.jetlinks.community.auth.entity.ThirdPartyUserBindEntity;
import org.jetlinks.community.auth.web.request.ThirdPartyBindUserInfo;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

@WebFluxTest(ThirdPartyUserController.class)
class ThirdPartyUserControllerTest extends TestJetLinksController {


    @Test
    void test() {

        String type = "wx-corp";
        String provider = "corp1";

        client.patch()
              .uri("/user/third-party/{type}/{provider}", type, provider)
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(ThirdPartyBindUserInfo.of(
                  "test","test", "用户1", "u1"
              ))
              .exchange()
              .expectStatus()
              .is2xxSuccessful();

        client.get()
              .uri("/user/third-party/{type}/{provider}", type, provider)
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBody()
              .jsonPath("$[0].userId").isEqualTo("test")
              .jsonPath("$[0].providerName").isEqualTo("用户1")
              .jsonPath("$[0].thirdPartyUserId").isEqualTo("u1");

        client.get()
              .uri("/user/third-party/me")
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBody()
              .jsonPath("$[0].userId").isEqualTo("test")
              .jsonPath("$[0].providerName").isEqualTo("用户1")
              .jsonPath("$[0].thirdPartyUserId").isEqualTo("u1")
        ;

        client.delete()
              .uri("/user/third-party/me/{id}", ThirdPartyUserBindEntity.generateId(
                  type, provider, "u1"
              ))
              .exchange()
              .expectStatus()
              .is2xxSuccessful();

        client.get()
              .uri("/user/third-party/me")
              .exchange()
              .expectStatus()
              .is2xxSuccessful()
              .expectBodyList(ThirdPartyUserBindEntity.class)
              .hasSize(0)
        ;
    }

}