package org.jetlinks.community.notify.wechat.corp.request;

import org.jetlinks.community.notify.wechat.corp.response.ApiResponse;
import org.jetlinks.core.command.Command;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/91201">企业内部开发>服务端API</a>
 * @since 2.0
 */
public abstract class ApiRequest<T extends ApiResponse> implements Command<Mono<T>> {

    public abstract Mono<T> execute(WebClient client);

}
