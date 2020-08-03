package org.jetlinks.community.gateway.external.socket;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
import org.hswebframework.web.authorization.token.UserToken;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.jetlinks.community.gateway.external.Message;
import org.jetlinks.community.gateway.external.MessagingManager;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@AllArgsConstructor
@Slf4j
public class WebSocketMessagingHandler implements WebSocketHandler {

    private final MessagingManager messagingManager;

    private final UserTokenManager userTokenManager;

    private final ReactiveAuthenticationManager authenticationManager;

    // /messaging/{token}
    @Override
    @Nonnull
    public Mono<Void> handle(@Nonnull WebSocketSession session) {
        String[] path = session.getHandshakeInfo().getUri().getPath().split("[/]");
        if (path.length == 0) {
            return session.send(Mono.just(session.textMessage(JSON.toJSONString(
                Message.error("auth", null, "错误的请求")
            )))).then(session.close(CloseStatus.BAD_DATA));
        }
        String token = path[path.length - 1];

        Map<String, Disposable> subs = new ConcurrentHashMap<>();

        return userTokenManager.getByToken(token)
            .map(UserToken::getUserId)
            .flatMap(authenticationManager::getByUserId)
            .switchIfEmpty(session
                .send(Mono.just(session.textMessage(JSON.toJSONString(
                    Message.authError()
                ))))
                .then(session.close(CloseStatus.BAD_DATA))
                .then(Mono.empty()))
            .flatMap(auth -> session
                .receive()
                .doOnNext(message -> {
                    try {
                        MessagingRequest request = JSON.parseObject(message.getPayloadAsText(), MessagingRequest.class);
                        if (request == null || request.getType() == MessagingRequest.Type.ping) {
                            return;
                        }
                        if (StringUtils.isEmpty(request.getId())) {
                            session
                                .send(Mono.just(session.textMessage(JSON.toJSONString(
                                    Message.error(request.getType().name(), null, "id不能为空")
                                )))).subscribe();
                            return;
                        }
                        if (request.getType() == MessagingRequest.Type.sub) {
                            //重复订阅
                            if (subs.containsKey(request.getId())) {
                                return;
                            }
                            subs.put(request.getId(), messagingManager
                                .subscribe(SubscribeRequest.of(request, auth))
                                .onErrorResume(err -> Mono.just(Message.error(request.getId(), request.getTopic(), err.getMessage())))
                                .map(msg -> session.textMessage(JSON.toJSONString(msg)))
                                .doOnComplete(() -> {
                                    subs.remove(request.getId());
                                    Mono.just(session.textMessage(JSON.toJSONString(Message.complete(request.getId()))))
                                        .as(session::send)
                                        .subscribe();
                                })
                                .flatMap(msg -> session.send(Mono.just(msg)))
                                .subscribe()
                            );

                        } else if (request.getType() == MessagingRequest.Type.unsub) {
                            Optional.ofNullable(subs.remove(request.getId()))
                                .ifPresent(Disposable::dispose);
                        } else {
                            session.send(Mono.just(session.textMessage(JSON.toJSONString(
                                Message.error(request.getId(), request.getTopic(), "不支持的类型:" + request.getType())
                            )))).subscribe();
                        }
                    } catch (Exception e) {
                        log.warn(e.getMessage(),e);
                        session.send(Mono.just(session.textMessage(JSON.toJSONString(
                            Message.error("illegal_argument", null, "消息格式错误")
                        )))).subscribe();
                    }
                })
                .then())
            .doFinally(r -> {
                subs.values().forEach(Disposable::dispose);
                subs.clear();
            });

    }
}
