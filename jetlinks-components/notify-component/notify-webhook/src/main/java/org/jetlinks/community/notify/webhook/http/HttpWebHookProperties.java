package org.jetlinks.community.notify.webhook.http;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
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
