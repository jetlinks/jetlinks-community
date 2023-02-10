package org.jetlinks.community.notify.dingtalk.corp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.dingtalk.corp.CorpUser;
import org.jetlinks.community.notify.dingtalk.corp.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * @author zhouhao
 * @see <a href="https://open.dingtalk.com/document/orgapp-server/queries-the-simple-information-of-a-department-user">获取部门用户基础信息</a>
 * @since 2.0
 */
@AllArgsConstructor
public class GetUserRequest extends ApiRequest<Flux<CorpUser>> {

    private final String departmentId;

    @Override
    public Flux<CorpUser> execute(WebClient client) {
        return this
            .doRequest(0, client)
            .flatMapIterable(Response::getList);
    }

    private Flux<Response> doRequest(int pageIndex, WebClient client) {
        return client
            .post()
            .uri("/topapi/user/listsimple")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters
                      .fromFormData("dept_id", departmentId)
                      .with("cursor", String.valueOf(pageIndex))
                      .with("size", "100")
            )
            .retrieve()
            .bodyToMono(Response.class)
            .flatMapMany(response -> {
                //下一页
                if (response.hasMore && response.nextCursor > 0) {
                    return Flux
                        .concat(
                            Mono.just(response),
                            doRequest(response.nextCursor, client)
                        );
                }
                return Mono.just(response);
            });
    }

    @Getter
    @Setter
    public static class Response extends ApiResponse {
        @JsonProperty
        @JsonAlias("has_more")
        private boolean hasMore;

        @JsonProperty
        @JsonAlias("next_cursor")
        private int nextCursor;

        @JsonProperty
        @JsonAlias("result")
        private Result result;

        public List<CorpUser> getList() {
            return result == null ? Collections.emptyList() : result.list;
        }

        @Getter
        @Setter
        public static class Result {

            @JsonProperty
            @JsonAlias("has_more")
            private boolean hasMore;

            @JsonProperty
            @JsonAlias("list")
            private List<CorpUser> list;

            public List<CorpUser> getList() {
                return list == null ? Collections.emptyList() : list;
            }
        }
    }
}
