package org.jetlinks.community.notify.dingtalk.corp;

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
import java.util.stream.Collectors;

@Getter
@Setter
public class DingTalkMessageTemplate extends AbstractTemplate<DingTalkMessageTemplate> {

    public static final String USER_ID_LIST_KEY = "userIdList";

    public static final String DEPARTMENT_ID_LIST_KEY = "departmentIdList";

    /**
     * 应用ID
     */
    @NotBlank
    private String agentId;

    /**
     * 接收者的userid列表，最大用户列表长度100。
     */
    private String userIdList;

    /**
     * 接收者的部门id列表，最大列表长度20。
     * <p>
     * 接收者是部门ID时，包括子部门下的所有用户。
     */
    private String departmentIdList;

    /**
     * 是否发送给企业全部用户。
     * <p>
     * 说明 当设置为false时必须指定userid_list或dept_id_list其中一个参数的值。
     */
    private boolean toAllUser;

    @NotBlank
    private String message;

    @Nonnull
    @Override
    protected List<VariableDefinition> getEmbeddedVariables() {
        if (toAllUser || StringUtils.hasText(userIdList) || StringUtils.hasText(departmentIdList)) {
            return Collections.emptyList();
        }
        List<VariableDefinition> variables = new ArrayList<>(2);
        variables.add(
            VariableDefinition
                .builder()
                .id(USER_ID_LIST_KEY)
                .name("收信人")
                .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.userType)
                .type(StringType.ID)
                .build()
        );
        variables.add(
            VariableDefinition
                .builder()
                .id(DEPARTMENT_ID_LIST_KEY)
                .name("收信人部门")
                .expand(NotifyVariableBusinessConstant.businessId,
                        NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.orgType)
                .type(StringType.ID)
                .build()
        );
        return variables;
    }

    @SneakyThrows
    public Mono<BodyInserters.FormInserter<String>> createFormInserter(BodyInserters.FormInserter<String> inserter,
                                                                       Values context,
                                                                       ConfigKey<String> relationPropertyPath) {
        return this.createUserIdList(context, relationPropertyPath)
            .map(userIdList -> {
                inserter.with("agent_id", this.getAgentId())
                    .with("to_all_user", String.valueOf(toAllUser))
                    .with("msg", this.createMessage(context));
                if (StringUtils.hasText(userIdList)) {
                    inserter.with("userid_list", userIdList);
                }
                String deptIdList = this.createDepartmentIdList(context);
                if (StringUtils.hasText(deptIdList)) {
                    inserter.with("dept_id_list", deptIdList);
                }

                return inserter;
            });
    }

    public Mono<UriComponentsBuilder> createUriParameter(UriComponentsBuilder builder,
                                                         Values context,
                                                         ConfigKey<String> relationPropertyPath) {
        return this.createUserIdList(context, relationPropertyPath)
            .map(userIdList -> {
                builder.queryParam("agent_id", this.getAgentId())
                    .queryParam("to_all_user", String.valueOf(toAllUser))
                    .queryParam("msg", this.createMessage(context));
                if (StringUtils.hasText(userIdList)) {
                    builder.queryParam("userid_list", userIdList);
                }
                String deptIdList = this.createDepartmentIdList(context);
                if (StringUtils.hasText(deptIdList)) {
                    builder.queryParam("dept_id_list", deptIdList);
                }

                return builder;
            });
    }

    public Mono<String> createUserIdList(Values context, ConfigKey<String> relationPropertyPath) {
        return VariableSource
            .resolveValue(USER_ID_LIST_KEY, context.getAllValues(), relationPropertyPath)
            .map(String::valueOf)
            .filter(StringUtils::hasText)
            .defaultIfEmpty(userIdList == null ? "" : userIdList)
            .collect(Collectors.joining(","));
    }

    public String createDepartmentIdList(Values context) {
        return get(this.getDepartmentIdList(), DEPARTMENT_ID_LIST_KEY, context.getAllValues());
    }

    public String createMessage(Values context) {
        JSONObject json = new JSONObject();
        json.put("msgtype", "text");
        json.put("text", Collections.singletonMap("content", render(message, context.getAllValues())));
        return json.toJSONString();
    }

}
