package org.jetlinks.community.notify.wechat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
public enum WechatProvider implements Provider {
    corpMessage("微信企业消息通知")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

}
