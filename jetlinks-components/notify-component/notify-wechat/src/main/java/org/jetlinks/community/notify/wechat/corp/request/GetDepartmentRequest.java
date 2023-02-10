package org.jetlinks.community.notify.wechat.corp.request;

import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetDepartmentResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90208">企业内部开发>服务端API>通讯录管理>部门管理>获取部门列表</a>
 * @since 2.0
 */
public class GetDepartmentRequest extends ApiRequest<GetDepartmentResponse> {

    @Override
    public Mono<GetDepartmentResponse> execute(WebClient client) {
        return client
            .post()
            .uri("/cgi-bin/department/list")
            .retrieve()
            .bodyToMono(GetDepartmentResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
