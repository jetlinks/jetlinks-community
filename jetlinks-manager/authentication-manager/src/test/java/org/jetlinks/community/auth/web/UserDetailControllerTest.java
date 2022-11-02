package org.jetlinks.community.auth.web;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.service.request.SaveUserDetailRequest;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.community.test.web.TestAuthentication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息接口.
 *
 * @author zhangji
 * @version 1.11 2021/9/29
 */
@DisplayName("用户信息接口：UserDetailController")
@WebFluxTest({UserDetailController.class, WebFluxUserController.class})
class UserDetailControllerTest extends TestJetLinksController {
    public static final String BASE_URI = "/user/detail";
    private             String userId;

    @Override
    protected void initAuth(TestAuthentication authentication) {
        super.initAuth(authentication);

        // 初始化用户ID
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(authentication.getUser().getUsername());
        userEntity.generateId();
        userId = userEntity.getId();

        // 修改当前用户的ID（和数据库保持一致）
        ((SimpleUser) authentication.getUser()).setId(userEntity.getId());
    }

    /**
     * 保存用户
     */
    @BeforeEach
    void addUser() {
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", "test");
        userMap.put("username", "test");
        userMap.put("password", "password123456");
        userMap.put("type", "test");

        Boolean result = client.patch()
            .uri("/user")
            .bodyValue(userMap)
            .exchange()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result);
    }

    @Test
    void test() {
        // 保存当前用户详情
        SaveUserDetailRequest saveRequest = new SaveUserDetailRequest();
        saveRequest.setName("测试用户");
        saveRequest.setDescription("单元测试-保存");
        saveRequest.setEmail("test@email.com");
        saveRequest.setTelephone("023-88888888");

        client.put()
            .uri(BASE_URI)
            .bodyValue(saveRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        // 获取当前登录用户详情
        client.get()
            .uri(BASE_URI)
            .exchange()
            .expectBody()
            .jsonPath("$.name").isEqualTo(saveRequest.getName())
            .jsonPath("$.email").isEqualTo(saveRequest.getEmail())
            .jsonPath("$.telephone").isEqualTo(saveRequest.getTelephone())
            .jsonPath("$.description").isEqualTo(saveRequest.getDescription());

        // 删除用户
        Boolean deleteResult = client.delete()
            .uri("/user/{id:.+}", userId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(deleteResult);
        Assertions.assertTrue(deleteResult);

        // 获取当前登录用户详情（用户数据不存在，返回默认数据）
        UserDetail userDetail = client.get()
            .uri(BASE_URI)
            .exchange()
            .expectBody(UserDetail.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(userDetail);
        Authentication
            .currentReactive()
            .map(i -> i.getUser().getName().equals(userDetail.getName()))
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }
}
