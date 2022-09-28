package org.jetlinks.community.notify.wechat.corp;

import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.community.notify.wechat.WechatProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.notify.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
public class WechatNotifierProvider implements NotifierProvider, TemplateProvider {

    private final WebClient.Builder clientBuilder;

    private final TemplateManager templateManager;


    public WechatNotifierProvider(TemplateManager templateManager, WebClient.Builder builder) {
        this.templateManager = templateManager;
        this.clientBuilder = builder;
    }

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("通知配置", "")
        .add("corpId", "corpId", "", new StringType().expand(ConfigMetadataConstants.required.value(true)))
        .add("corpSecret", "corpSecret", "", new StringType());

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("模版配置", "")
        .add("agentId", "应用ID", "", new StringType().expand(ConfigMetadataConstants.required.value(true)))
        .add("toUser", "收信人ID", "与部门ID不能同时为空", new StringType())
        .add("toParty", "收信部门ID", "与收信人ID不能同时为空", new StringType())
        .add("toTag", "按标签推送", "", new StringType())
        .add("message", "内容", "最大不超过500字", new StringType().expand(ConfigMetadataConstants.maxLength.value(500L)));

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.weixin;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return WechatProvider.corpMessage;
    }

    @Override
    public Mono<WechatMessageTemplate> createTemplate(TemplateProperties properties) {
        return Mono.fromSupplier(() ->new WechatMessageTemplate().with(properties).validate())
            .as(LocaleUtils::transform);
    }

    @Nonnull
    @Override
    public Mono<WechatCorpNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.defer(() -> {
            WechatCorpProperties wechatCorpProperties = FastBeanCopier.copy(properties.getConfiguration(), new WechatCorpProperties());
            return Mono.just(new WechatCorpNotifier(properties.getId(), clientBuilder, ValidatorUtils.tryValidate(wechatCorpProperties), templateManager));
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
