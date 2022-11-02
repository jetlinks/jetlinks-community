package org.jetlinks.community.auth.web;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.community.test.web.TestAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 输入描述.
 *
 * @author zhangji
 * @version 1.11 2021/9/29
 */
@DisplayName("用户管理：WebFluxUserController")
@WebFluxTest(WebFluxUserController.class)
public class WebFluxUserControllerTest extends TestJetLinksController {
    public static final String BASE_URI  = "/user";
    public              String userId;
    public static final String TENANT_ID = "tenant-test";

    @Override
    protected void initAuth(TestAuthentication authentication) {
        super.initAuth(authentication);

        // 添加租户
//        authentication.addDimension(TenantDimensionType.tenant.getId(), TENANT_ID);

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

    /**
     * 删除用户
     */
    @AfterEach
    void deleteUser() {
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
    }


    @Test
    void testUsernameValidate() {
        client.post()
            .uri("/user/username/_validate")
            .bodyValue("test")
            .exchange()
            .expectBody()
            .jsonPath("$.passed")
            .isEqualTo(false);
    }

    @Test
    void testPasswordValidate() {
        client.post()
            .uri("/user/password/_validate")
            .bodyValue("1111")
            .exchange()
            .expectBody()
            .jsonPath("$.passed")
            .isEqualTo(false);
    }

    @Test
    void testPasswordReset() {
        client.post()
            .uri("/user/" + userId + "/password/_reset")
            .bodyValue("pwd1qaz2wsx")
            .exchange()
            .expectBody(Integer.class)
            .isEqualTo(1);
    }

    @Test
    void test() {
        // 修改用户
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", userId);
        userMap.put("name", "test-update");

        Boolean result = client.patch()
            .uri("/user")
            .bodyValue(userMap)
            .exchange()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result);

        // 使用POST方式分页动态查询(不返回总数)
        List<UserEntity> userList = client.post()
            .uri(BASE_URI + "/_query/no-paging")
            .bodyValue(QueryParamEntity.of())
            .exchange()
            .expectBodyList(UserEntity.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertFalse(CollectionUtils.isEmpty(userList));

        // 使用GET方式分页动态查询(不返回总数)
        List<UserEntity> userList2 = client.get()
            .uri(BASE_URI + "/_query/no-paging")
            .exchange()
            .expectBodyList(UserEntity.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertFalse(CollectionUtils.isEmpty(userList2));

        // 使用POST方式分页动态查询
        PagerResult<UserEntity> userPager = client.post()
            .uri(BASE_URI + "/_query")
            .bodyValue(QueryParamEntity.of())
            .exchange()
            .expectBody(new ParameterizedTypeReference<PagerResult<UserEntity>>() {
            })
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(userPager);
        Assertions.assertFalse(CollectionUtils.isEmpty(userPager.getData()));

        // 使用GET方式分页动态查询
        PagerResult<UserEntity> userPager2 = client.get()
            .uri(BASE_URI + "/_query")
            .exchange()
            .expectBody(new ParameterizedTypeReference<PagerResult<UserEntity>>() {
            })
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(userPager2);
        Assertions.assertFalse(CollectionUtils.isEmpty(userPager2.getData()));

        // 使用POST方式查询总数
        Integer total = client.post()
            .uri(BASE_URI + "/_count")
            .bodyValue(QueryParamEntity.of())
            .exchange()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(total);
        Assertions.assertTrue(total > 0);

        // 使用GET方式查询总数
        Integer total2 = client.get()
            .uri(BASE_URI + "/_count")
            .exchange()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(total2);
        Assertions.assertTrue(total2 > 0);

        // 修改用户状态
        Boolean stateResult = client.put()
            .uri(BASE_URI + "/{id:.+}/{state}", userId, "0")
            .exchange()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(stateResult);
        Assertions.assertTrue(stateResult);

        // 根据ID查询
        UserEntity user = client.get()
            .uri(BASE_URI + "/{id:.+}", userId)
            .exchange()
            .expectBody(UserEntity.class)
            .returnResult()
            .getResponseBody();
        Assertions.assertNotNull(user);
        Assertions.assertEquals(userId, user.getId());
    }
}
