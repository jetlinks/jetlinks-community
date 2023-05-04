package org.jetlinks.community.notify.sms.aliyun;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.types.StringType;
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
 * 阿里云短信模版
 *
 * @since 1.3
 */
@Getter
@Setter
public class AliyunSmsTemplate extends AbstractTemplate<AliyunSmsTemplate> {

    public static final String PHONE_NUMBER_KEY = "phoneNumber";

    //签名名称
    @NotBlank(message = "[signName]不能为空")
    private String signName;

    //模版编码
    @NotBlank(message = "[code]不能为空")
    private String code;

    //为空时,则表示从变量中传入
    // @NotBlank(message = "[phoneNumber]不能为空")
    private String phoneNumber;

    private Map<String, String> param;

    public String getCode(Map<String, Object> ctx) {
        //code不支持变量
        return getCode();
    }

    public Flux<String> getPhoneNumber(Map<String, Object> ctx) {
        if (StringUtils.hasText(this.getPhoneNumber())) {
            return Flux.just(this.getPhoneNumber());
        }
        //如果没有指定固定值,则从上下文中获取
        return RelationUtils
            .resolve(PHONE_NUMBER_KEY, ctx, RelationConstants.UserProperty.telephone)
            .map(String::valueOf);
    }

    public String getSignName(Map<String, Object> ctx) {
        //签名不支持变量
        return getSignName();
    }

    public String createTtsParam(Map<String, Object> ctx) {
        Map<String, VariableDefinition> variables = getVariables();
        return JSON.toJSONString(Maps.filterEntries(
            renderMap(ctx),
            e -> variables.containsKey(e.getKey())));
    }

    @Override
    @Nonnull
    protected List<VariableDefinition> getEmbeddedVariables() {
        //指定了固定的收信人
        if (StringUtils.hasText(phoneNumber)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(
            VariableDefinition
                .builder()
                .id(PHONE_NUMBER_KEY)
                .name("收信人")
                .description("收信人手机号码")
                .expand(NotifyVariableBusinessConstant.businessId,
                    NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.userType)
                .type(StringType.ID)
                .required(true)
                .build()
        );
    }
}
