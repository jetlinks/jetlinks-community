package org.jetlinks.community.notify.wechat;

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
public class WechatMessageTemplate implements Template {

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


    @SneakyThrows
    public BodyInserters.FormInserter<String> createFormInserter(BodyInserters.FormInserter<String> inserter, Values context) {
        inserter.with("agentid", this.getAgentId())
                .with("msgtype","text")
                .with("text",this.createMessage(context));
        if (StringUtils.hasText(toUser)) {
            inserter.with("touser", this.createUserIdList(context));
        }
        if (StringUtils.hasText(toParty)) {
            inserter.with("toparty", this.createDepartmentIdList(context));
        }
        return inserter;

    }

    public String createJsonRequest(Values context){
        JSONObject json=new JSONObject();
        json.put("agentid",getAgentId());
        json.put("msgtype","text");
        json.put("text",Collections.singletonMap("content",ExpressionUtils.analytical(message, context.getAllValues(), "spel")));

        if (StringUtils.hasText(toUser)) {
            json.put("touser", this.createUserIdList(context));
        }
        if (StringUtils.hasText(toParty)) {
            json.put("toparty", this.createDepartmentIdList(context));
        }

        return json.toJSONString();
    }


    public UriComponentsBuilder createUriParameter(UriComponentsBuilder builder, Values context){
        builder.queryParam("agentid", this.getAgentId())
                .queryParam("msgtype","text")
                .queryParam("text",this.createMessage(context));
        if (StringUtils.hasText(toUser)) {
            builder.queryParam("touser", this.createUserIdList(context));
        }
        if (StringUtils.hasText(toParty)) {
            builder.queryParam("toparty", this.createDepartmentIdList(context));
        }
        return builder;
    }

    public String createUserIdList(Values context) {
        if (StringUtils.isEmpty(toUser)) {
            return toUser;
        }
        return ExpressionUtils.analytical(toUser, context.getAllValues(), "spel");
    }

    public String createDepartmentIdList(Values context) {
        if (StringUtils.isEmpty(toParty)) {
            return toParty;
        }
        return ExpressionUtils.analytical(toParty, context.getAllValues(), "spel");
    }

    public String createMessage(Values context) {
        JSONObject json = new JSONObject();
        json.put("content", ExpressionUtils.analytical(message, context.getAllValues(), "spel"));
        return json.toJSONString();
    }

}
