package org.jetlinks.community.notify.wechat.corp.request;

import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetUserResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90216">企业内部开发>服务端API>通讯录管理>成员管理>获取部门成员详情</a>
 * @since 2.0
 */
@AllArgsConstructor
public class GetUserRequest extends ApiRequest<GetUserResponse> {

    private final String departmentId;
    private final boolean fetchChild;

    @Override
    public Mono<GetUserResponse> execute(WebClient client) {
        return client
            .post()
            .uri("/cgi-bin/user/list", builder -> builder
                .queryParam("department_id", departmentId)
                .queryParam("fetch_child", fetchChild ? "1" : 0)
                .build())
            .retrieve()
            .bodyToMono(GetUserResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
