package org.jetlinks.community.gateway.supports;

import lombok.Getter;
import org.jetlinks.community.gateway.*;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class LocalMessageConnection implements
    MessageConnection,
    MessageSubscriber,
    MessagePublisher {

    private final List<Runnable> listener = new CopyOnWriteArrayList<>();

    @Getter
    private final String id;

    private final boolean shareCluster;

    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    private final EmitterProcessor<TopicMessage> processor = EmitterProcessor.create(false);

    private final FluxSink<TopicMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final EmitterProcessor<Subscription> subscriptionProcessor = EmitterProcessor.create(false);
    private final EmitterProcessor<Subscription> unsubscriptionProcessor = EmitterProcessor.create(false);

    public LocalMessageConnection(String id, boolean shareCluster) {
        this.id = id;
        this.shareCluster = shareCluster;
    }

    public void addSubscription(Subscription subscription) {
        subscriptionProcessor.onNext(subscription);
    }

    public void removeSubscription(Subscription subscription) {
        unsubscriptionProcessor.onNext(subscription);
    }

    @Override
    public void onDisconnect(Runnable disconnectListener) {
        if (disconnected.get()) {
            disconnectListener.run();
            return;
        }
        listener.add(disconnectListener);
    }

    @Override
    public void disconnect() {
        listener.forEach(Runnable::run);
        listener.clear();
        disconnected.set(true);
        processor.onComplete();
        subscriptionProcessor.onComplete();
        unsubscriptionProcessor.onComplete();
    }

    @Override
    public boolean isAlive() {
        return !disconnected.get();
    }

    @Nonnull
    @Override
    public Mono<Void> publish(@Nonnull TopicMessage message) {
        return Mono.fromRunnable(() -> {
            if (processor.hasDownstreams()) {
                sink.next(message);
            }
        });
    }

    public Flux<TopicMessage> onLocalMessage() {
        return processor.map(Function.identity());
    }

    @Nonnull
    @Override
    public Flux<TopicMessage> onMessage() {

        return Flux.empty();
    }

    @Nonnull
    @Override
    public Flux<Subscription> onSubscribe() {
        return subscriptionProcessor.map(Function.identity());
    }

    @Nonnull
    @Override
    public Flux<Subscription> onUnSubscribe() {
        return unsubscriptionProcessor.map(Function.identity());
    }

    @Override
    public boolean isShareCluster() {
        return shareCluster;
    }
}
