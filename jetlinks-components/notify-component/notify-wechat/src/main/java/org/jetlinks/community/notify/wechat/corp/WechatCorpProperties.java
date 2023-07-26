package org.jetlinks.community.notify.wechat.corp;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.NotifierProperties;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class WechatCorpProperties {

    @NotBlank(message = "corpId不能为空")
    private String corpId;

    @NotBlank(message = "corpSecret不能为空")
    private String corpSecret;

    public static WechatCorpProperties of(NotifierProperties properties) {
        return FastBeanCopier.copy(properties.getConfiguration(), new WechatCorpProperties());
    }

}
