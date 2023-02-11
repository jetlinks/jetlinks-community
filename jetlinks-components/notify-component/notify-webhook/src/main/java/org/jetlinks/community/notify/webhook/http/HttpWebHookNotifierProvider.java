package org.jetlinks.community.notify.webhook.http;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.community.notify.webhook.WebHookProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
@AllArgsConstructor
public class HttpWebHookNotifierProvider implements NotifierProvider, TemplateProvider {

    private final TemplateManager templateManager;

    private final WebClient.Builder builder;

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.webhook;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return WebHookProvider.http;
    }

    @Override
    public Mono<HttpWebHookTemplate> createTemplate(TemplateProperties properties) {
        return Mono.just(new HttpWebHookTemplate().with(properties).validate())
            .as(LocaleUtils::transform);
    }

    @Nonnull
    @Override
    public Mono<? extends Notifier<? extends Template>> createNotifier(@Nonnull NotifierProperties properties) {

        HttpWebHookProperties hookProperties = FastBeanCopier.copy(properties.getConfiguration(),new HttpWebHookProperties());
        ValidatorUtils.tryValidate(hookProperties);

        WebClient.Builder client = builder.clone();

        client.baseUrl(hookProperties.getUrl());

        return Mono.just(new HttpWebHookNotifier(properties.getId(),
                                                 hookProperties,
                                                 client.build(),
                                                 templateManager))
            .as(LocaleUtils::transform);
    }
}
