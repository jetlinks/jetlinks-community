package org.jetlinks.community.notify.dingtalk;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.core.Values;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.util.Collections;

@Getter
@Setter
public class DingTalkMessageTemplate implements Template {

    /**
     * 应用ID
     */
    @NotBlank(message = "[agentId]不能为空")
    private String agentId;

    private String userIdList;

    private String departmentIdList;

    private boolean toAllUser;

    @NotBlank(message = "[message]不能为空")
    private String message;


    @SneakyThrows
    public BodyInserters.FormInserter<String> createFormInserter(BodyInserters.FormInserter<String> inserter, Values context) {
        inserter.with("agent_id", this.getAgentId())
                .with("to_all_user", String.valueOf(toAllUser))
                .with("msg",this.createMessage(context));
        if (StringUtils.hasText(userIdList)) {
            inserter.with("userid_list", this.createUserIdList(context));
        }
        if (StringUtils.hasText(departmentIdList)) {
            inserter.with("dept_id_list", this.createDepartmentIdList(context));
        }
        return inserter;

    }

    public UriComponentsBuilder createUriParameter(UriComponentsBuilder builder, Values context){
        builder.queryParam("agent_id", this.getAgentId())
                .queryParam("to_all_user", String.valueOf(toAllUser))
                .queryParam("msg",this.createMessage(context));
        if (StringUtils.hasText(userIdList)) {
            builder.queryParam("userid_list", this.createUserIdList(context));
        }
        if (StringUtils.hasText(departmentIdList)) {
            builder.queryParam("dept_id_list", this.createDepartmentIdList(context));
        }
        return builder;
    }

    public String createUserIdList(Values context) {
        if (StringUtils.isEmpty(userIdList)) {
            return userIdList;
        }
        return ExpressionUtils.analytical(userIdList, context.getAllValues(), "spel");
    }

    public String createDepartmentIdList(Values context) {
        if (StringUtils.isEmpty(departmentIdList)) {
            return departmentIdList;
        }
        return ExpressionUtils.analytical(departmentIdList, context.getAllValues(), "spel");
    }

    public String createMessage(Values context) {
        JSONObject json = new JSONObject();
        json.put("msgtype", "text");
        json.put("text", Collections.singletonMap("content",ExpressionUtils.analytical(message, context.getAllValues(), "spel")));
        return json.toJSONString();
    }

}
