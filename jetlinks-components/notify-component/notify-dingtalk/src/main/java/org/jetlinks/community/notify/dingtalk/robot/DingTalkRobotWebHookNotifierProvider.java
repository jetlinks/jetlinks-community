package org.jetlinks.community.notify.dingtalk.robot;

import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.dingtalk.DingTalkProvider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.notify.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
public class DingTalkRobotWebHookNotifierProvider implements NotifierProvider, TemplateProvider {

    private final WebClient client;

    private final TemplateManager templateManager;

    public DingTalkRobotWebHookNotifierProvider(TemplateManager templateManager, WebClient.Builder builder) {
        this.templateManager = templateManager;
        this.client = builder.build();
    }

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("通知配置", "")
        .add("url", "Webhook", "", new StringType().expand(ConfigMetadataConstants.required.value(true)));

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("模版配置", "")
        .add("messageType", "消息类型", "", new EnumType()
            .addElement(EnumType.Element.of("text", "text"))
            .addElement(EnumType.Element.of("link", "link"))
            .addElement(EnumType.Element.of("markdown", "markdown"))
            .expand(ConfigMetadataConstants.required.value(true)))
        .add("text", "文本消息", "", new ObjectType()
            .addProperty("content", "文本内容", StringType.GLOBAL))
        .add("link", "连接消息", "", new ObjectType()
            .addProperty("title", "标题", StringType.GLOBAL)
            .addProperty("picUrl", "图片地址", new FileType())
            .addProperty("text", "正文", StringType.GLOBAL)
            .addProperty("messageUrl", "消息连接", StringType.GLOBAL)
        )
        .add("markdown", "markdown消息", "推送到全部用户", new ObjectType()
            .addProperty("title", "标题", StringType.GLOBAL)
            .addProperty("text", "正文", StringType.GLOBAL)
        )
        .add("at", "At", "At指定的人", new ObjectType()
            .addProperty("atMobiles", "按手机号码at", new ArrayType().elementType(StringType.GLOBAL))
            .addProperty("atUserIds", "按用户IDat", new ArrayType().elementType(StringType.GLOBAL))
            .addProperty("atAll", "是否at全员", BooleanType.GLOBAL)
        );

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

    @Override
    public Mono<DingTalkWebHookTemplate> createTemplate(TemplateProperties properties) {
        return Mono.fromSupplier(() -> new DingTalkWebHookTemplate().with(properties).validate())
            .as(LocaleUtils::transform);
    }

    @Nonnull
    @Override
    public Mono<DingTalkRobotWebHookNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.fromSupplier(() -> {
            String url = properties
                .getString("url")
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("url can not be null"));
            return new DingTalkRobotWebHookNotifier(
                properties.getId(), templateManager, url, client
            );
        })
            .as(LocaleUtils::transform);
    }

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return templateConfig;
    }
}
