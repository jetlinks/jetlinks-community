package org.jetlinks.community.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DefaultNotifyType implements NotifyType {

    sms("短信"),
    email("邮件"),
    voice("语音"),
    dingTalk("钉钉"),
    weixin("微信"),
    webhook("WebHook");

    private final String name;

    @Override
    public String getId() {
        return name();
    }
}
