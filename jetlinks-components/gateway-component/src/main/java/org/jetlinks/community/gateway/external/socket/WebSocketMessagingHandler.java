package org.jetlinks.community.gateway.external.socket;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
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
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@AllArgsConstructor
public class WebSocketMessagingHandler implements WebSocketHandler {

    private final MessagingManager messagingManager;

    private final UserTokenManager userTokenManager;

    private final ReactiveAuthenticationManager authenticationManager;

    // /messaging/{token}
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String[] path = session.getHandshakeInfo().getUri().getPath().split("[/]");
        if (path.length == 0) {
            return session.send(Mono.just(session.textMessage(JSON.toJSONString(
                Message.error("auth", null, "错误的请求")
            )))).then(session.close(CloseStatus.BAD_DATA));
        }
        String token = path[path.length - 1];

        Set<String> disposable = new ConcurrentSkipListSet<>();

        return userTokenManager.getByToken(token)
            .map(UserToken::getUserId)
            .flatMap(authenticationManager::getByUserId)
            .switchIfEmpty(session
                .send(Mono.just(session.textMessage(JSON.toJSONString(
                    Message.error("auth", null, "认证失败")
                ))))
                .then(session.close(CloseStatus.BAD_DATA))
                .then(Mono.empty()))
            .flatMap(auth -> session
                .receive()
                .flatMap(message -> {
                    MessagingRequest request = JSON.parseObject(message.getPayloadAsText(), MessagingRequest.class);
                    if (StringUtils.isEmpty(request.getId())) {
                        return session
                            .send(Mono.just(session.textMessage(JSON.toJSONString(
                                Message.error(request.getType().name(), null, "id不能为空")
                            ))));
                    }
                    if (request.getType() == MessagingRequest.Type.sub) {
                        //重复订阅
                        if (disposable.contains(request.getId())) {
                            return Mono.empty();
                        }
                        disposable.add(request.getId());
                        return session.send(messagingManager
                            .subscribe(SubscribeRequest.of(request, auth))
                            .onErrorResume(err -> Mono.just(Message.error(request.getId(), request.getTopic(), err.getMessage())))
                            .takeWhile(r -> disposable.contains(request.getId()))
                            .switchIfEmpty(Mono.fromSupplier(() -> Message.error(request.getId(), request.getTopic(), "不支持的Topic")))
                            .map(msg -> session.textMessage(JSON.toJSONString(msg)))
                            .doOnComplete(() -> disposable.remove(request.getId()))
                        );
                    } else if (request.getType() == MessagingRequest.Type.unsub) {
                        return Mono.fromRunnable(() -> disposable.remove(request.getId()));
                    } else {
                        return session.send(Mono.just(session.textMessage(JSON.toJSONString(
                            Message.error(request.getId(), request.getTopic(), "不支持的类型:" + request.getType())
                        ))));
                    }
                }).then())
            .doFinally(r -> disposable.clear());

    }
}
