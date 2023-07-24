package org.jetlinks.community.notify.dingtalk.corp;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.notify.AbstractNotifier;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.Provider;
import org.jetlinks.community.notify.dingtalk.DingTalkProvider;
import org.jetlinks.community.notify.dingtalk.corp.request.ApiRequest;
import org.jetlinks.community.notify.dingtalk.corp.request.GetAccessTokenRequest;
import org.jetlinks.community.notify.dingtalk.corp.request.GetUserAccessTokenRequest;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.core.Values;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.community.notify.dingtalk.corp.response.AccessTokenResponse;
import org.jetlinks.community.notify.dingtalk.corp.response.ApiResponse;
import org.jetlinks.community.relation.RelationConstants;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;

/**
 * @author zhouhao
 * @see <a href="https://open.dingtalk.com/document/orgapp-server/asynchronous-sending-of-enterprise-session-messages">发送工作通知</a>
 * @since 1.0
 */
@Slf4j
public class DingTalkNotifier extends AbstractNotifier<DingTalkMessageTemplate> implements CommandSupport, ExchangeFilterFunction {

    private final WebClient client;

    private final DingTalkProperties properties;

    private volatile Mono<String> token;

    @Getter
    private final String notifierId;

    public DingTalkNotifier(String id, WebClient.Builder client, DingTalkProperties properties, TemplateManager templateManager) {
        super(templateManager);
        this.client = client
            .clone()
            .baseUrl("https://oapi.dingtalk.com")
            .filter(this)
            .build();
        this.properties = properties;
        this.notifierId = id;
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.dingTalk;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return DingTalkProvider.dingTalkMessage;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull DingTalkMessageTemplate template, @Nonnull Values context) {
        // 关系对象属性
        String thirdPartyType = getType().getId() + "_" + getProvider().getId();
        String thirdPartyProvider = getNotifierId();
        ConfigKey<String> relationPropertyPath = RelationConstants.UserProperty.thirdParty(thirdPartyType, thirdPartyProvider);

        return template
            .createFormInserter(BodyInserters.fromFormData(new LinkedMultiValueMap<>()), context, relationPropertyPath)
            .flatMap(formInserter -> client
                .post()
                .uri("/topapi/message/corpconversation/asyncsend_v2")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formInserter)
                .retrieve()
                .bodyToMono(ApiResponse.class))
            .then();
    }

    public Mono<AccessTokenResponse> getUserAccessToken(String code) {
        return execute(new GetUserAccessTokenRequest(properties.getAppKey(), properties.getAppSecret(), code));
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
                        .doOnNext(AccessTokenResponse::assertSuccess)
                        .map(AccessTokenResponse::getAccessToken);
                }
            }
        }
        return token;
    }

    private Mono<AccessTokenResponse> requestToken() {
        return execute(new GetAccessTokenRequest(properties.getAppKey(), properties.getAppSecret()));
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }

    @Nonnull
    @Override
    public <R> R execute(@Nonnull Command<R> command) {
        if (command instanceof ApiRequest) {
            return ((ApiRequest<R>) command).execute(client);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull ClientRequest request, @Nonnull ExchangeFunction next) {
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
}
