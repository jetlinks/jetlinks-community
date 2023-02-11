package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

@Getter
@Setter
public class AccessTokenResponse extends ApiResponse{

    @JsonProperty
    @JsonAlias("access_token")
    private String accessToken;

    @JsonProperty
    @JsonAlias("expires_in")
    private int expiresIn;

    @Override
    public void assertSuccess() {
        super.assertSuccess();
        Assert.hasText(accessToken,"error.qy_wx_token_response_error");
    }
}
