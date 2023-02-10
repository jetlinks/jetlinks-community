package org.jetlinks.community.strategy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class StaticStrategyManager<S
    extends Strategy>
    implements StrategyManager<S> {

    private final Map<String, Mono<S>> strategies = new ConcurrentHashMap<>();

    public void addStrategy(S strategy) {
        this.addStrategy(strategy.getId(), Mono.just(strategy));
    }

    public void addStrategy(String strategyId, Mono<S> providerMono) {
        strategies.put(strategyId, providerMono);
    }

    @Override
    public final Mono<S> getStrategy(String strategyId) {
        return strategies.getOrDefault(strategyId, Mono.empty());
    }

    @Override
    public final Flux<S> getStrategies() {
        return Flux.concat(strategies.values());
    }

    protected final <T> Mono<T> doWithMono(String strategy, Function<S, Mono<T>> executor) {
        return this
            .getStrategy(strategy)
            .switchIfEmpty(onStrategyNotFound(strategy))
            .flatMap(executor);
    }

    protected final <T> Flux<T> doWithFlux(String strategy, Function<S, Flux<T>> executor) {
        return this
            .getStrategy(strategy)
            .switchIfEmpty(onStrategyNotFound(strategy))
            .flatMapMany(executor);
    }

    protected <T> Mono<T> onStrategyNotFound(String strategy){
        return Mono.empty();
    }
}
