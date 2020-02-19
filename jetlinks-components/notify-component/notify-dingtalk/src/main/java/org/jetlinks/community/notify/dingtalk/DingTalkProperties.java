package org.jetlinks.community.notify.dingtalk;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class DingTalkProperties {

    @NotBlank(message = "appKey不能为空")
    private String appKey;

    @NotBlank(message = "appSecret不能为空")
    private String appSecret;


}
