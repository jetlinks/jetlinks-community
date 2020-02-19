package org.jetlinks.community.notify.sms.provider;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetlinks.core.Values;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.PasswordType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
@Profile({"dev","test"})
public class Hy2046SmsSenderProvider implements NotifierProvider, TemplateProvider, Provider {


    private WebClient webClient = WebClient.builder()
            .baseUrl("http://sms10692.com/v2sms.aspx")
            .build();

    @Autowired
    private TemplateManager templateManager;

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.sms;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return this;
    }

    static DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("宏衍2046短信配置", "")
            .add("userId", "userId", "用户ID", new StringType())
            .add("username", "用户名", "用户名", new StringType())
            .add("password", "密码", "密码", new PasswordType());

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return PlainTextSmsTemplate.templateConfig;
    }

    @Override
    public Mono<? extends Template> createTemplate(TemplateProperties properties) {
        return Mono.fromSupplier(() -> JSON.parseObject(properties.getTemplate(), PlainTextSmsTemplate.class));
    }

    @Nonnull
    @Override
    public Mono<Hy2046SmsSender> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.defer(() -> {
            String userId = (String) properties.getConfigOrNull("userId");
            String username = (String) properties.getConfigOrNull("username");
            String password = (String) properties.getConfigOrNull("password");
            Assert.hasText(userId, "短信配置错误,缺少userId");
            Assert.hasText(username, "短信配置错误,缺少username");
            Assert.hasText(password, "短信配置错误,缺少password");
            return Mono.just(new Hy2046SmsSender(userId, username, password));
        });
    }

    @Override
    public String getId() {
        return "hy2046";
    }

    @Override
    public String getName() {
        return "宏衍2046";
    }

    class Hy2046SmsSender extends AbstractNotifier<PlainTextSmsTemplate> {

        String userId;
        String username;
        String password;

        public Hy2046SmsSender(String userId, String username, String password) {
            super(templateManager);
            this.userId = userId;
            this.username = username;
            this.password = password;
        }

        @Nonnull
        @Override
        public NotifyType getType() {
            return DefaultNotifyType.sms;
        }

        @Nonnull
        @Override
        public Provider getProvider() {
            return Hy2046SmsSenderProvider.this;
        }

        @Nonnull
        @Override
        public Mono<Void> close() {
            return Mono.empty();
        }


        @Nonnull
        @Override
        public Mono<Void> send(@Nonnull PlainTextSmsTemplate template, @Nonnull Values context) {
            return Mono.defer(() -> {
                String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
                String sign = DigestUtils.md5Hex(username.concat(password).concat(ts));
                String[] sendTo = template.getSendTo(context.getAllValues());

                String mobile = String.join(",", sendTo);
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("userid", userId);
                formData.add("timestamp", ts);
                formData.add("sign", sign);
                formData.add("mobile", mobile);
                formData.add("content", template.getTextSms(context.getAllValues()));
                formData.add("action", "send");
                formData.add("rt", "json");

                return webClient.post()
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(formData))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(map -> {
                            if (Integer.valueOf(sendTo.length).equals(map.get("SuccessCounts"))) {
                                return true;
                            }
                            throw new RuntimeException("发送短信失败:" + map.get("Message"));
                        });

            }).then();
        }

    }
}

