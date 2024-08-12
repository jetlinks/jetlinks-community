package org.jetlinks.community.rule.engine.commons;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;

import javax.annotation.Nonnull;

public class ShakeLimitFlux<T> extends FluxOperator<T, ShakeLimitResult<T>> {

    private final int threshold;
    private final int completeFirst;

    protected ShakeLimitFlux(int threshold,
                             int completeFirst,
                             Flux<? extends T> source) {
        super(source);
        this.threshold = threshold;
        this.completeFirst = completeFirst;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super ShakeLimitResult<T>> actual) {

    }
}
