package org.jetlinks.community.notify.dingtalk.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessTokenResponse extends ApiResponse {

    @JsonProperty
    @JsonAlias("access_token")
    private String accessToken;

    @JsonProperty
    @JsonAlias("expires_in")
    private int expiresIn;


}
