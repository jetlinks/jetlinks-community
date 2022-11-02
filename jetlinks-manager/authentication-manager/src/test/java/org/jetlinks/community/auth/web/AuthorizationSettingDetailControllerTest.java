package org.jetlinks.community.auth.web;

import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 输入描述.
 *
 * @author zhangji
 * @version 1.11 2021/9/28
 */
@DisplayName("权限分配：AuthorizationSettingDetailController")
@WebFluxTest(AuthorizationSettingDetailController.class)
class AuthorizationSettingDetailControllerTest extends TestJetLinksController {

    public static final String BASE_URI    = "/autz-setting/detail";
    public static final String TARGET_TYPE = "user";
    public static final String TARGET      = "test-operator";

    @Test
    void test() {
        Boolean result = client.post()
            .uri(BASE_URI + "/_save")
            .body(Flux.just(authSettingDetail()), AuthorizationSettingDetail.class)
            .exchange()
            .expectBody(Boolean.class)
            .returnResult()
            .getResponseBody();

        assertEquals(true, result);

        AuthorizationSettingDetail authSettingDetail = client.get()
            .uri(BASE_URI + String.format("/%s/%s", TARGET_TYPE, TARGET))
            .exchange()
            .expectBody(AuthorizationSettingDetail.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(authSettingDetail);
        assertEquals(TARGET_TYPE, authSettingDetail.getTargetType());
        assertEquals(TARGET, authSettingDetail.getTargetId());
    }

    AuthorizationSettingDetail authSettingDetail() {
        AuthorizationSettingDetail authSettingDetail = new AuthorizationSettingDetail();
        authSettingDetail.setTargetType(TARGET_TYPE);
        authSettingDetail.setTargetId(TARGET);
        authSettingDetail.setMerge(true);
        authSettingDetail.setPriority(1);
        authSettingDetail.setPermissionList(Arrays.asList(
            AuthorizationSettingDetail.PermissionInfo.of("1", new HashSet<>(Arrays.asList("query"))),
            AuthorizationSettingDetail.PermissionInfo.of("2", new HashSet<>(Arrays.asList("query", "update")))
        ));
        return authSettingDetail;
    }


//    @Override
//    protected void initAuth(TestAuthentication authentication) {
//        super.initAuth(authentication);
//        authentication.addDimension(TARGET_TYPE, TARGET);
//    }
}
