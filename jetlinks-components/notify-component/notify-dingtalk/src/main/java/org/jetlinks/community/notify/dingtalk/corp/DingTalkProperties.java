package org.jetlinks.community.notify.dingtalk.corp;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class DingTalkProperties {

    @NotBlank
    private String appKey;

    @NotBlank
    private String appSecret;


}
