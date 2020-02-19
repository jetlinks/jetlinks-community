package org.jetlinks.community.notify.wechat;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class WechatCorpProperties {

    @NotBlank(message = "corpId不能为空")
    private String corpId;

    @NotBlank(message = "corpSecret不能为空")
    private String corpSecret;


}
