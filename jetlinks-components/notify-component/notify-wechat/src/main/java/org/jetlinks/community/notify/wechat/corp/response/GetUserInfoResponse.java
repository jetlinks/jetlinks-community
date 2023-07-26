package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 *
 * @author zhangji 2023/6/8
 * @since 2.1
 */
@Getter
public class GetUserInfoResponse extends ApiResponse {

    @JsonProperty
    @JsonAlias("userid")
    private String userId;

}
