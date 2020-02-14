package org.jetlinks.community.gateway.spring;

import lombok.Getter;
import org.jetlinks.community.gateway.MessageConnection;
import org.jetlinks.community.gateway.MessageSubscriber;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.gateway.TopicMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

public class SpringMessageConnection implements MessageConnection,
    MessageSubscriber {

    @Getter
    private String id;

    private MessageListener listener;

    private List<Subscription> subscription;

    private boolean shareCluster;

    public SpringMessageConnection(String id,
                                   List<Subscription> subscription,
                                   MessageListener listener,
                                   boolean shareCluster) {
        this.listener = listener;
        this.id = id;
        this.subscription = subscription;
        this.shareCluster = shareCluster;
    }

    @Override
    public void onDisconnect(Runnable disconnectListener) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Nonnull
    @Override
    public Mono<Void> publish(@Nonnull TopicMessage message) {
        return listener.onMessage(message);
    }

    @Nonnull
    @Override
    public Flux<Subscription> onSubscribe() {
        return Flux.fromIterable(subscription);
    }

    @Nonnull
    @Override
    public Flux<Subscription> onUnSubscribe() {
        return Flux.empty();
    }

    @Override
    public boolean isShareCluster() {
        return shareCluster;
    }
}
