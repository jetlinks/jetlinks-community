package org.jetlinks.community.notify.dingtalk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
public enum DingTalkProvider implements Provider {
    dingTalkMessage("钉钉消息通知")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

}
