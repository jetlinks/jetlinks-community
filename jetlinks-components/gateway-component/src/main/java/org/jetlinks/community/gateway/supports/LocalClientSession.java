package org.jetlinks.community.gateway.supports;

import lombok.Getter;
import org.jetlinks.community.gateway.ClientSession;
import org.jetlinks.community.gateway.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class LocalClientSession implements ClientSession {

    @Getter
    private String id;

    @Getter
    private String clientId;

    private Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();


    public LocalClientSession(String id) {
        this.id = id;
        this.clientId = id;
    }

    @Override
    public boolean isPersist() {
        return false;
    }

    @Override
    public Flux<Subscription> getSubscriptions() {
        return Flux.fromIterable(subscriptions.values());
    }

    @Override
    public Mono<Void> addSubscription(Subscription subscription) {
        return Mono.fromRunnable(() -> subscriptions.put(subscription.getTopic(), subscription));
    }

    @Override
    public Mono<Void> removeSubscription(Subscription subscription) {
        return Mono.fromRunnable(() -> subscriptions.remove(subscription.getTopic()));
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void close() {

    }
}
