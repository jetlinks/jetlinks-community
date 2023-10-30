package org.jetlinks.community.notify.voice.aliyun;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.notify.NotifyVariableBusinessConstant;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.jetlinks.community.relation.RelationConstants;
import org.jetlinks.community.relation.utils.RelationUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

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
    @Deprecated
    private String ttsCode;

    @Schema(description = "通知模版ID")
    private String templateCode;

    private String calledShowNumbers;

    private String calledNumber;

    private TemplateType templateType = TemplateType.tts;

    @Schema(description = "通知播放次数")
    private int playTimes = 1;

    private Map<String, String> ttsParam;

    public String createTtsParam(Map<String, Object> ctx) {
        Map<String, VariableDefinition> variables = getVariables();

        return JSON.toJSONString(Maps.filterEntries(renderMap(ctx),
                                                    e -> variables.containsKey(e.getKey())));
    }

    public Flux<String> getCalledNumber(Map<String, Object> ctx) {
        if (StringUtils.hasText(this.getCalledNumber())) {
            return Flux.just(this.getCalledNumber());
        }
        //如果没有指定固定值,则从上下文中获取
        return RelationUtils
            .resolve(CALLED_NUMBER_KEY, ctx, RelationConstants.UserProperty.telephone)
            .map(String::valueOf);
    }

    public String getTemplateCode() {
        if (templateCode == null) {
            return ttsCode;
        }
        return templateCode;
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
                .name("被叫号码")
                .description("收信人手机号码")
                .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.userType)
                .required(true)
                .build()
        );
    }

    public enum TemplateType {
        voice, tts
    }
}
