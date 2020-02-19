package org.jetlinks.community.notify.voice.aliyun;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.template.Template;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 阿里云通知模版
 *
 * https://help.aliyun.com/document_detail/114035.html?spm=a2c4g.11186623.6.561.3d1b3c2dGMXAmk
 */
@Getter
@Setter
public class AliyunVoiceTemplate implements Template {

    @NotBlank(message = "[ttsCode]不能为空")
    private String ttsCode;

    @NotBlank(message = "[calledShowNumbers]不能为空")
    private String calledShowNumbers;

    @NotBlank(message = "[calledNumber]不能为空")
    private String calledNumber;

    private int playTimes = 1;

    private Map<String,String> ttsParam;

    public String createTtsParam(Map<String,Object> ctx){

        return JSON.toJSONString(ctx);
    }
}
