package org.jetlinks.community.auth.dimension;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.jetlinks.community.authorize.FastSerializableAuthentication;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.topic.Topics;
import org.springframework.context.event.EventListener;
import reactor.core.Disposable;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;

@Slf4j
public class UserAuthenticationEventPublisher {
    private final EventBus eventBus;

    private final Sinks.Many<String> asyncPublish = Sinks
        .many()
        .multicast()
        .directBestEffort();

    private final Disposable disposable;

    public UserAuthenticationEventPublisher(EventBus eventBus) {
        this.eventBus = eventBus;

        disposable = asyncPublish
            .asFlux()
            .bufferTimeout(32, Duration.ofSeconds(1), HashSet::new)
            .onBackpressureBuffer(
                10240,
                dropped -> log.warn("user authentication changed event dropped:{}", dropped.size()),
                BufferOverflowStrategy.DROP_LATEST)
            .concatMap(list -> publish0(list)
                .as(MonoTracer.create("/user/authentication/changed-async"))
                .onErrorResume(err -> {
                    log.warn("publish user authentication changed error", err);
                    return Mono.empty();
                }))
            .subscribe();

    }

    public void shutdown() {
        disposable.dispose();
    }

    @EventListener
    public void handleEvent(ClearUserAuthorizationCacheEvent event) {
        if (event.isAll()) {
            return;
        }
        event.async(publish(event.getUserId()));
    }

    private Mono<Void> publish0(Collection<String> userIdList) {
        return Flux
            .fromIterable(userIdList)
            .flatMapDelayError(
                userId -> ReactiveAuthenticationHolder
                    .get(userId)
                    .flatMap(auth -> eventBus
                        .publish(Topics
                                     .Authentications
                                     .userAuthenticationChanged(auth.getUser().getId()),
                                 FastSerializableAuthentication.of(auth, true)
                        ))
                    .as(MonoTracer.create("/user/" + userId + "/authentication/changed")),
                4, 4)
            .then();
    }

    private Mono<Void> publish(Collection<String> userIdList) {
        if (userIdList.size() == 1) {
            return publish0(userIdList);
        }
        for (String userId : userIdList) {
            asyncPublish.emitNext(userId, Reactors.emitFailureHandler());
        }
        return Mono.empty();
    }


}
