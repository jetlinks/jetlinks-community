package org.jetlinks.community.notify.dingtalk;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
@Generated
public enum DingTalkProvider implements Provider {
    dingTalkMessage("钉钉消息通知"),
    dingTalkRobotWebHook("群机器人")
    ;

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
