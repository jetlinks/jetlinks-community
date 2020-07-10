package org.jetlinks.community.notify.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
public enum SmsProvider implements Provider {

    aliyunSms("阿里云短信服务")
    ;
    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
