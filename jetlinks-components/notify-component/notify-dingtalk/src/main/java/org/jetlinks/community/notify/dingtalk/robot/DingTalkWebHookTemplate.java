package org.jetlinks.community.notify.dingtalk.robot;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.notify.NotifyVariableBusinessConstant;
import org.jetlinks.community.notify.template.AbstractTemplate;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DingTalkWebHookTemplate extends AbstractTemplate<DingTalkWebHookTemplate> {
    private MessageType messageType;

    private Text text;
    private Markdown markdown;
    private Link link;
    private At at;

    public DingTalkWebHookTemplate validate() {
        Assert.notNull(messageType, "messageType can not be null");

        switch (messageType) {
            case text:
                Assert.notNull(text, "text can not be null");
                break;
            case markdown:
                Assert.notNull(markdown, "markdown can not be null");
                break;
            case link:
                Assert.notNull(link, "link can not be null");
                break;
            default:
                throw new UnsupportedOperationException("unsupported messageType " + messageType);
        }
        return this;
    }

    public JSONObject toJson(Map<String, Object> context) {
        validate();
        JSONObject json = new JSONObject();
        json.put("msgtype", messageType.name());
        if (at != null) {
            json.put("at", at.render(this, context).toJson());
        }
        switch (messageType) {
            case text:
                json.put("text", text.render(this, context).toJson());
                break;
            case markdown:
                json.put("markdown", markdown.render(this, context).toJson());
                break;
            case link:
                json.put("link", link.render(this, context).toJson());
                break;
        }
        return json;
    }

    public enum MessageType {
        text,
        link,
        markdown
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class At {
        private List<String> atMobiles;
        private List<String> atUserIds;
        private boolean atAll;

        public At render(Template template, Map<String, Object> context) {
            return new At(template.render(atMobiles, context),
                          template.render(atUserIds, context),
                          atAll);
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("atMobiles", atMobiles);
            json.put("atUserIds", atUserIds);
            json.put("isAtAll", atAll);
            return json;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Text {
        private String content;

        public Text render(Template template, Map<String, Object> context) {
            return new Text(template.render(content, context));
        }

        public JSONObject toJson() {
            return new JSONObject(Collections.singletonMap("content", content));
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Markdown {
        private String title;
        private String text;

        public Markdown render(Template template, Map<String, Object> context) {
            return new Markdown(template.render(title, context),
                                template.render(text, context));
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("title", title);
            json.put("text", text);
            return json;
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Link {
        public static final String MESSAGE_URL_KEY = "messageUrl";
        public static final String PIC_URL_KEY = "picUrl";

        private String title;
        private String picUrl;
        private String text;
        private String messageUrl;

        public Link render(Template template, Map<String, Object> context) {
            return new Link(template.render(title, context),
                            template.get(picUrl, PIC_URL_KEY, context),
                            template.render(text, context),
                            template.get(getMessageUrl(), MESSAGE_URL_KEY, context)
            );
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("title", title);
            json.put("text", text);
            json.put("picUrl", picUrl);
            json.put("messageUrl", messageUrl);
            return json;
        }
    }


    @Nonnull
    @Override
    protected List<VariableDefinition> getEmbeddedVariables() {
        if (MessageType.link.equals(getMessageType())) {
            Link link = getLink();
            if (link == null) {
                return super.getEmbeddedVariables();
            }

            List<VariableDefinition> variableDefinitions = new ArrayList<>(2);
            if (StringUtils.isEmpty(link.getMessageUrl())) {
                variableDefinitions.add(VariableDefinition
                                            .builder()
                                            .id(Link.MESSAGE_URL_KEY)
                                            .name("内容链接")
                                            .description("内容消息链接")
                                            .expand(NotifyVariableBusinessConstant.businessId,
                                                    NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.linkType)
                                            .type(StringType.ID)
                                            .required(true)
                                            .build());
            }
            if (StringUtils.isEmpty(link.getPicUrl())) {
                variableDefinitions.add(VariableDefinition
                                            .builder()
                                            .id(Link.PIC_URL_KEY)
                                            .name("图片链接")
                                            .description("图片链接")
                                            .expand(NotifyVariableBusinessConstant.businessId,
                                                NotifyVariableBusinessConstant.NotifyVariableBusinessTypes.fileType)
                                            .type(StringType.ID)
                                            .required(true)
                                            .build());
            }
            return variableDefinitions;
        }

        return super.getEmbeddedVariables();
    }
}
