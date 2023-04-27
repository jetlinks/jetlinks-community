package org.jetlinks.community.notify.dingtalk.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.BusinessException;

@Getter
@Setter
public class ApiResponse {

    @JsonProperty
    @JsonAlias("request_id")
    private String requestId;

    @JsonProperty
    @JsonAlias("errcode")
    private int errorCode;

    @JsonProperty
    @JsonAlias("errmsg")
    private String errorMessage;

    public boolean isSuccess() {
        return errorCode == 0;
    }

    public void assertSuccess() {
        if (!isSuccess()) {
            throw new BusinessException("error.dingtalk_api_request_error", 500, errorMessage);
        }
    }
}
