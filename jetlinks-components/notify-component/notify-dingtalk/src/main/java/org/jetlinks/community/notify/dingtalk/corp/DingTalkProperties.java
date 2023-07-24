package org.jetlinks.community.notify.dingtalk.corp;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.NotifierProperties;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class DingTalkProperties {

    @NotBlank
    private String appKey;

    @NotBlank
    private String appSecret;

    public static DingTalkProperties of(NotifierProperties properties){
        return FastBeanCopier.copy(properties.getConfiguration(), new DingTalkProperties());
    }
}
