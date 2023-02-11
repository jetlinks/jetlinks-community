package org.jetlinks.community.strategy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StrategyManager<S extends Strategy> {

    Mono<S> getStrategy(String provider);

    Flux<S> getStrategies();

}
