package org.jetlinks.community.notify.wechat.corp;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.notify.AbstractNotifier;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.Provider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.wechat.WechatProvider;
import org.jetlinks.community.notify.wechat.corp.request.AccessTokenRequest;
import org.jetlinks.community.notify.wechat.corp.request.ApiRequest;
import org.jetlinks.community.notify.wechat.corp.request.NotifyRequest;
import org.jetlinks.community.notify.wechat.corp.response.AccessTokenResponse;
import org.jetlinks.community.notify.wechat.corp.response.NotifyResponse;
import org.jetlinks.core.Values;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.community.relation.RelationConstants;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;

@Slf4j
public class WechatCorpNotifier extends AbstractNotifier<WechatMessageTemplate> implements CommandSupport, ExchangeFilterFunction {

    private final WebClient apiClient;

    private final WechatCorpProperties properties;

    @Getter
    private final String notifierId;

    private volatile Mono<String> token;

    public WechatCorpNotifier(String id, WebClient.Builder builder, WechatCorpProperties properties, TemplateManager templateManager) {
        super(templateManager);
        //api client
        this.apiClient = builder
            .clone()
            .filter(this)
            .baseUrl("https://qyapi.weixin.qq.com")
            .build();

        this.properties = properties;
        this.notifierId = id;
    }

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

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull WechatMessageTemplate template, @Nonnull Values context) {
        // 关系对象属性
        String thirdPartyType = getType().getId() + "_" + getProvider().getId();
        String thirdPartyProvider = getNotifierId();
        ConfigKey<String> relationPropertyPath = RelationConstants.UserProperty.thirdParty(thirdPartyType, thirdPartyProvider);

        return template
            .createJsonRequest(context, relationPropertyPath)
            .flatMap(jsonRequest -> this.execute(new NotifyRequest(jsonRequest)))
            .doOnNext(NotifyResponse::assertSuccess)
            .then();
    }

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public <R> R execute(@Nonnull Command<R> command) {
        if (command instanceof ApiRequest) {
            return (R) ((ApiRequest<?>) command).execute(apiClient);
        }
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        this.token = null;
        return Mono.empty();
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull ClientRequest request,
                                       @Nonnull ExchangeFunction next) {
        if (request.url().getPath().endsWith("gettoken")) {
            return next.exchange(request);
        }
        //自动填充access_token
        return this
            .getToken()
            .flatMap(token -> next
                .exchange(
                    ClientRequest
                        .from(request)
                        .url(UriComponentsBuilder
                                 .fromUri(request.url())
                                 .queryParam("access_token", token)
                                 .build()
                                 .toUri())
                        .build()
                ));
    }


    private Mono<String> getToken() {
        if (token == null) {
            synchronized (this) {
                if (token == null) {
                    token = this
                        .requestToken()
                        .cache(val -> Duration.ofSeconds(Math.max(3600, val.getExpiresIn()) - 60),
                               err -> Duration.ZERO,
                               () -> Duration.ZERO)
                        .map(AccessTokenResponse::getAccessToken);
                }
            }
        }
        return token;
    }

    private Mono<AccessTokenResponse> requestToken() {
        return new AccessTokenRequest(properties.getCorpId(), properties.getCorpSecret())
            .execute(apiClient)
            .doOnNext(AccessTokenResponse::assertSuccess);
    }
}
