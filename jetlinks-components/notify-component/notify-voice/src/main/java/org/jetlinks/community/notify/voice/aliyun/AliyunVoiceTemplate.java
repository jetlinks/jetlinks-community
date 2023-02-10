package org.jetlinks.community.notify.voice.aliyun;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 阿里云语音验证码通知模版
 * <p>
 * https://help.aliyun.com/document_detail/114035.html?spm=a2c4g.11186623.6.561.3d1b3c2dGMXAmk
 */
@Getter
@Setter
public class AliyunVoiceTemplate extends AbstractTemplate<AliyunVoiceTemplate> {
    public static final String CALLED_NUMBER_KEY = "calledNumber";

    @Schema(description = "通知模版ID")
    @NotBlank(message = "[ttsCode]不能为空")
    private String ttsCode;

    private String calledShowNumbers;

    private String calledNumber;

    @Schema(description = "通知播放次数")
    private int playTimes = 1;

    private Map<String, String> ttsParam;

    public String createTtsParam(Map<String, Object> ctx) {

        return JSON.toJSONString(ctx);
    }

    public String getCalledNumber(Map<String, Object> ctx) {
        return get(CALLED_NUMBER_KEY, ctx, this::getCalledNumber);
    }

    @Nonnull
    @Override
    protected List<VariableDefinition> getEmbeddedVariables() {
        //指定了固定的收信人
        if (StringUtils.hasText(calledNumber)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(
            VariableDefinition
                .builder()
                .id(CALLED_NUMBER_KEY)
                .name("收信人")
                .description("收信人手机号码")
                .required(true)
                .build()
        );
    }
}
