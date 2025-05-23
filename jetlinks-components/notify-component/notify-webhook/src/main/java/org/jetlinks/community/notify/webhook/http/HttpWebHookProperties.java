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
package org.jetlinks.community.notify.webhook.http;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
public class HttpWebHookProperties {
    @Schema(description = "请求根地址,如: https://host/api")
    @NotBlank
    @URL
    private String url;

    @Schema(description = "请求头")
    private List<Header> headers;

    //todo 认证方式


    @Getter
    @Setter
    public static class Header {
        private String key;

        private String value;
    }
}
