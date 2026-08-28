/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.net.URI;

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
            bodyUriSpec.uri(resolveTemplateUri(template.getUrl()));
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

    URI resolveTemplateUri(String url) {
        URI uri = URI.create(url);
        // 模板只能补充相对路径，目标主机始终由具有配置权限的通知器配置决定。
        if (uri.isAbsolute() || uri.getHost() != null || url.startsWith("//")) {
            throw new IllegalArgumentException("WebHook template URL must be relative");
        }
        return uri;
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }
}
