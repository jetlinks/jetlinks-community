package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.exception.BusinessException;

@Getter
@Setter
public class ApiResponse {

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
            throw new BusinessException("error.qy_wx_request_error", 500, errorMessage);
        }
    }
}
