package org.jetlinks.community.notify.sms;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.notify.sms.aliyun.AliyunSmsNotifier;
import org.jetlinks.community.notify.sms.aliyun.AliyunSmsTemplate;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
@Generated
public enum SmsProvider implements Provider {

    /**
     * @see AliyunSmsTemplate
     * @see AliyunSmsNotifier
     */
    aliyunSms("阿里云短信服务")
    ;
    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
