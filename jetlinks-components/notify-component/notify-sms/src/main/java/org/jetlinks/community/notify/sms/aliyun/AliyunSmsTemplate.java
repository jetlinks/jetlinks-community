package org.jetlinks.community.notify.sms.aliyun;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.template.Template;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 阿里云短信模版
 *
 * @since 1.3
 */
@Getter
@Setter
public class AliyunSmsTemplate implements Template {

    //签名名称
    @NotBlank(message = "[signName]不能为空")
    private String signName;

    //模版编码
    @NotBlank(message = "[code]不能为空")
    private String code;

    @NotBlank(message = "[phoneNumber]不能为空")
    private String phoneNumber;

    private Map<String, String> param;

    public String createTtsParam(Map<String, Object> ctx) {

        return JSON.toJSONString(ctx);
    }
}
