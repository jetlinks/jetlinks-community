package org.jetlinks.community.auth.web;

import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.ReactiveAuthenticationInitializeService;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;


@WebFluxTest(RoleController.class)
class RoleControllerTest extends TestJetLinksController {

    @Autowired
    private ReactiveAuthenticationInitializeService initializeService;

    @Autowired
    private ReactiveUserService userService;

    @Test
    void testBind() {
        RoleEntity role = new RoleEntity();
        role.setId("test");
        role.setName("Test");

        UserEntity user = new UserEntity();
        user.setName("Test");
        user.setUsername("test");
        user.setPassword("Test123456");

        userService.saveUser(Mono.just(user))
            .block();

        client
            .post()
            .uri("/role")
            .bodyValue(role)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .post()
            .uri("/role/{id}/users/_bind", role.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singletonList(user.getId()))
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        initializeService
            .initUserAuthorization(user.getId())
            .as(StepVerifier::create)
            .expectNextMatches(auth -> auth.hasDimension(DefaultDimensionType.role, role.getId()))
            .verifyComplete();

        client
            .post()
            .uri("/role/{id}/users/_unbind", role.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singletonList(user.getId()))
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        initializeService
            .initUserAuthorization(user.getId())
            .as(StepVerifier::create)
            .expectNextMatches(auth -> !auth.hasDimension(DefaultDimensionType.role, role.getId()))
            .verifyComplete();
    }

}