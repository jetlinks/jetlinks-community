package org.jetlinks.community.notify.webhook.http;

import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.community.notify.AbstractNotifier;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.Provider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.webhook.WebHookProvider;
import org.jetlinks.core.Values;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class HttpWebHookNotifier extends AbstractNotifier<HttpWebHookTemplate> {
    private final String id;

    private final WebClient webClient;

    private final HttpWebHookProperties properties;

    public HttpWebHookNotifier(String id,
                               HttpWebHookProperties properties,
                               WebClient webClient,
                               TemplateManager templateManager) {
        super(templateManager);
        this.id = id;
        this.properties = properties;
        this.webClient = webClient;
    }

    @Override
    public String getNotifierId() {
        return id;
    }

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

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull HttpWebHookTemplate template,
                           @Nonnull Values context) {
        HttpMethod method = template.getMethod();
        WebClient.RequestBodyUriSpec bodyUriSpec = webClient
            .method(template.getMethod());

        if (StringUtils.hasText(template.getUrl())) {
            bodyUriSpec.uri(template.getUrl());
        }
        if (method == HttpMethod.POST
            || method == HttpMethod.PUT
            || method == HttpMethod.PATCH) {
            String body = template.resolveBody(context);
            if (null != body) {
                bodyUriSpec.bodyValue(body);
            }
        }

        bodyUriSpec.headers(headers -> {
            if (CollectionUtils.isNotEmpty(properties.getHeaders())) {
                for (HttpWebHookProperties.Header header : properties.getHeaders()) {
                    headers.add(header.getKey(), header.getValue());
                }
            }

            if (CollectionUtils.isNotEmpty(template.getHeaders())) {
                for (HttpWebHookProperties.Header header : template.getHeaders()) {
                    headers.add(header.getKey(), header.getValue());
                }
            }
        });

        return bodyUriSpec
            .retrieve()
            .bodyToMono(Void.class);
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }
}
