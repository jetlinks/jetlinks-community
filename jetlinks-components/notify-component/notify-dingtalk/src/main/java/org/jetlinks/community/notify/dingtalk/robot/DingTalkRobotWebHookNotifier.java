package org.jetlinks.community.notify.dingtalk.robot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.jetlinks.community.notify.AbstractNotifier;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.Provider;
import org.jetlinks.community.notify.dingtalk.DingTalkProvider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.core.Values;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

class DingTalkRobotWebHookNotifier extends AbstractNotifier<DingTalkWebHookTemplate> {
    @Getter
    private final String notifierId;

    private final String url;

    private final WebClient client;

    public DingTalkRobotWebHookNotifier(String notifierId,
                                        TemplateManager templateManager,
                                        String url,
                                        WebClient client) {
        super(templateManager);
        this.notifierId = notifierId;
        this.url = url;
        this.client = client;
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.dingTalk;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return DingTalkProvider.dingTalkRobotWebHook;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull DingTalkWebHookTemplate template,
                           @Nonnull Values context) {
        return client
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(template.toJson(context.getAllValues()))
            .retrieve()
            .bodyToMono(String.class)
            .doOnNext(str -> {
                JSONObject response = JSON.parseObject(str);
                if (0 != response.getIntValue("errcode")) {
                    throw new IllegalArgumentException(translateMessage(response.getString("errmsg")));
                }
            })
            .then();
    }

    public static String translateMessage(String message) {
        if (message != null && message.contains("keywords not in content")) {
            return "error.ding_webhook_keywords_not_in_content";
        }
        if (message != null && message.contains("not in whitelist")) {
            return "error.ding_ip_not_in_whitelist";
        }
        if (message != null && message.contains("token is not exist")) {
            return "error.ding_token_error";
        }
        return "error.ding_webhook_arg_error";
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }
}
