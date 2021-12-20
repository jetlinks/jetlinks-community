package org.jetlinks.community.auth.web;


import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(UserDetailController.class)
class UserDetailControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/user/detail";

    @Autowired
    private  ReactiveUserService userService;

    @Autowired
    private ReactiveRepository<UserEntity, String> repository;
    @Test
    void getCurrentLoginUserDetail() {


        UserEntity userEntity = new UserEntity();
        userEntity.setId("test");
        userEntity.setName("test");
        userEntity.setUsername("test");
        userEntity.setPassword("test");
        userEntity.setSalt("test");
        repository.save(userEntity).subscribe();
//        userService.saveUser(Mono.just(userEntity)).subscribe();
        UserDetail responseBody = client.get()
            .uri(BASE_URL)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(UserDetail.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
        assertNotNull(responseBody);
        assertEquals("test",responseBody.getName());

    }

    @Test
    void saveUserDetail() {
//        SaveUserDetailRequest saveUserDetailRequest = new SaveUserDetailRequest();
//        saveUserDetailRequest.setName("test");
//        saveUserDetailRequest.setAvatar("test");
//        saveUserDetailRequest.setTelephone("18996666666");
//        saveUserDetailRequest.setEmail("1149127931@qq.com");
        String s = "{\n" +
            "  \"name\": \"test\",\n" +
            "  \"email\": \"\",\n" +
            "  \"telephone\": \"\",\n" +
            "  \"avatar\": \"\",\n" +
            "  \"description\": \"\"\n" +
            "}";
        client.put()
            .uri(BASE_URL)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }
}