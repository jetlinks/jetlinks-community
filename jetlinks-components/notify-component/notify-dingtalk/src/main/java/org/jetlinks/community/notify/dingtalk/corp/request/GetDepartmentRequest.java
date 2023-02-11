package org.jetlinks.community.notify.dingtalk.corp.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.community.notify.dingtalk.corp.CorpDepartment;
import org.jetlinks.community.notify.dingtalk.corp.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * @see <a href="https://open.dingtalk.com/document/orgapp-server/obtain-the-department-list-v2">获取部门列表</a>
 */
@AllArgsConstructor
public class GetDepartmentRequest extends ApiRequest<Flux<CorpDepartment>> {

    private final String departmentId;

    private final boolean fetchChild;

    public GetDepartmentRequest() {
        this(null, true);
    }

    public GetDepartmentRequest(boolean fetchChild) {
        this(null, fetchChild);
    }

    @Override
    public Flux<CorpDepartment> execute(WebClient client) {
        return this
            .doRequest(departmentId, client)
            .flatMapIterable(Response::getResult);
    }

    private Flux<Response> doRequest(String departmentId, WebClient client) {
        return client
            .post()
            .uri("/topapi/v2/department/listsub")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(StringUtils.hasText(departmentId)
                      ? BodyInserters.fromFormData("dept_id", departmentId)
                      : BodyInserters.empty())
            .retrieve()
            .bodyToMono(Response.class)
            .flatMapMany(response -> {
                response.assertSuccess();
                //获取下级部门
                if (fetchChild && CollectionUtils.isNotEmpty(response.getResult())) {
                    return Flux
                        .concat(Mono.just(response),
                                Flux.fromIterable(response.getResult())
                                    .filter(dep -> StringUtils.hasText(dep.getId()))
                                    .flatMap(dep -> doRequest(dep.getId(), client)));
                }
                return Mono.just(response);
            })
            ;
    }

    @Getter
    @Setter
    public static class Response extends ApiResponse {

        @JsonProperty
        @JsonAlias("result")
        private List<CorpDepartment> result;

        public List<CorpDepartment> getResult() {
            return result == null ? Collections.emptyList() : result;
        }
    }
}
