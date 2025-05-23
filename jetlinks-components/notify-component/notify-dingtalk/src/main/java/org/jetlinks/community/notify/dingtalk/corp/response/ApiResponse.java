/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
