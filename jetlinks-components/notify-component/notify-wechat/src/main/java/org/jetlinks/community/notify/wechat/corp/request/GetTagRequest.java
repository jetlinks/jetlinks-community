package org.jetlinks.community.notify.wechat.corp.request;

import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetTagResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90216">企业内部开发>服务端API>通讯录管理>标签管理>获取标签列表</a>
 * @since 2.0
 */
public class GetTagRequest extends ApiRequest<GetTagResponse> {

    @Override
    public Mono<GetTagResponse> execute(WebClient client) {
        return client
            .post()
            .uri("/cgi-bin/tag/list")
            .retrieve()
            .bodyToMono(GetTagResponse.class)
            .doOnNext(ApiResponse::assertSuccess);
    }
}
