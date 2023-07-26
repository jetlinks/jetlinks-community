package org.jetlinks.community.notify.wechat.corp;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.community.notify.NotifyVariableBusinessConstant;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.relation.utils.VariableSource;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Setter
public class WechatMessageTemplate extends AbstractTemplate<WechatMessageTemplate> {

    public static final String TO_USER_KEY = "toUser";
    public static final String TO_PARTY_KEY = "toParty";
    public static final String TO_TAG_KEY = "toTag";

    /**
     * 应用ID
     */
    @NotBlank(message = "[agentId]不能为空")
    private String agentId;

    private String toUser;

    private String toParty;

    private String toTag;

    @NotBlank(message = "[message]不能为空")
    private String message;

    public Mono<String> getToUser(Map<String, Object> context, ConfigKey<String> relationPropertyPath) {
        return VariableSource
            .resolveValue(TO_USER_KEY, context, relationPropertyPath)
            .map(String::valueOf)
            .filter(StringUtils::hasText)
            .defaultIfEmpty(toUser == null ? "" : toUser)
            .collect(Collectors.joining(","));
    }

    public String getToParty(Map<String, Object> context) {
        return getTo(TO_PARTY_KEY, context, this::getToParty);
    }

    public String getToTag(Map<String, Object> context) {
        return getTo(TO_TAG_KEY, context, this::getToTag);
    }

    private String getTo(String key, Map<String, Object> context, Supplier<Object> defaultValue) {
        String toUser = get(key, context, defaultValue);
        if (toUser != null) {
            return toUser.replace(',', '|');
        }
        return null;
    }

    @Nonnull
    @Override
    protected List<VariableDefinition> getEmbeddedVariables() {

        if (StringUtils.hasText(toUser) || StringUtils.hasText(toParty) || StringUtils.hasText(toTag)) {
            return Collections.emptyList();
        }
        List<VariableDefinition> variables = new ArrayList<>(3);
        variables.add(
            VariableDefinition
                .builder()
                .id(TO_USER_KEY)
                .name("收信人")
                .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.userType)
                .type(StringType.ID)
                .build()
        );
        variables.add(
            VariableDefinition
                .builder()
                .id(TO_PARTY_KEY)
                .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.orgType)
                .type(StringType.ID)
                .name("收信人部门")
                .build()
        );
        variables.add(
            VariableDefinition
                .builder()
                .id(TO_TAG_KEY)
                .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.tagType)
                .type(StringType.ID)
                .name("收信人标签")
                .build()
        );
        return variables;
    }

    @SneakyThrows
    public Mono<BodyInserters.FormInserter<String>> createFormInserter(BodyInserters.FormInserter<String> inserter,
                                                                       Values context,
                                                                       ConfigKey<String> relationPropertyPath) {
        return getToUser(context.getAllValues(), relationPropertyPath)
            .map(toUser -> {
                inserter.with("agentid", this.getAgentId())
                    .with("msgtype", "text")
                    .with("text", this.createMessage(context));

                String toParty = getToParty(context.getAllValues());

                if (StringUtils.hasText(toUser)) {
                    inserter.with("touser", toUser);
                }
                if (StringUtils.hasText(toParty)) {
                    inserter.with("touser", toParty);
                }
                return inserter;
            });
    }

    public Mono<JSONObject> createJsonRequest(Values context, ConfigKey<String> relationPropertyPath) {
        return getToUser(context.getAllValues(), relationPropertyPath)
            .map(toUser -> {
                JSONObject json = new JSONObject();
                json.put("agentid", getAgentId());
                json.put("msgtype", "text");
                json.put("text", Collections.singletonMap("content", render(message, context.getAllValues())));
                String toParty = getToParty(context.getAllValues());
                String toTag = getToTag(context.getAllValues());

                if (StringUtils.hasText(toUser)) {
                    json.put("touser", toUser);
                }
                if (StringUtils.hasText(toParty)) {
                    json.put("toparty", toParty);
                }
                if (StringUtils.hasText(toTag)) {
                    json.put("totag", toTag);
                }

                return json;
            });
    }

    public Mono<UriComponentsBuilder> createUriParameter(UriComponentsBuilder builder,
                                                         Values context,
                                                         ConfigKey<String> relationPropertyPath) {
        return getToUser(context.getAllValues(), relationPropertyPath)
            .map(toUser -> {
                builder.queryParam("agentid", this.getAgentId())
                    .queryParam("msgtype", "text")
                    .queryParam("text", this.createMessage(context));
                String toParty = getToParty(context.getAllValues());
                String toTag = getToTag(context.getAllValues());

                if (StringUtils.hasText(toUser)) {
                    builder.queryParam("touser", toUser);
                }
                if (StringUtils.hasText(toParty)) {
                    builder.queryParam("toparty", toParty);
                }
                if (StringUtils.hasText(toTag)) {
                    builder.queryParam("totag", toTag);
                }

                return builder;
            });
    }

    public String createMessage(Values context) {
        JSONObject json = new JSONObject();
        json.put("content", render(message, context.getAllValues()));
        return json.toJSONString();
    }

}
