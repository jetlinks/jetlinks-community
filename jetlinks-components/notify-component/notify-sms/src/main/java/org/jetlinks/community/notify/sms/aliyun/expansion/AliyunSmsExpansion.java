package org.jetlinks.community.notify.sms.aliyun.expansion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 阿里云短信扩展信息
 *
 * @author bestfeng
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class AliyunSmsExpansion {


    @Schema(description = "签名列表集合")
    private List<SmsSign> smsSigns;

    @Schema(description = "模板列表集合")
    private List<SmsTemplate> smsTemplates;
}
