package org.jetlinks.community.utils.math;

import lombok.Generated;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.function.BiFunction;

public class RangeFlux<T, R> extends org.jetlinks.community.utils.math.MonoFromFluxOperator<T, R> {

    private final Comparator<T> comparator;

    private final BiFunction<T, T, R> resultMapper;

    protected RangeFlux(Flux<? extends T> source,
                        Comparator<T> comparator,
                        BiFunction<T, T, R> resultMapper) {
        super(source);
        this.comparator = comparator;
        this.resultMapper = resultMapper;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super R> coreSubscriber) {
        source.subscribe(new RangeSubscriber<>(coreSubscriber, comparator, resultMapper));
    }

    @Generated
    static final class RangeSubscriber<T, R> extends MathSubscriber<T, R> {

        private final Comparator<T> comparator;

        private final BiFunction<T, T, R> resultMapper;

        boolean hasValue;

        T min;
        T max;

        RangeSubscriber(CoreSubscriber<? super R> actual,
                        Comparator<T> comparator,
                        BiFunction<T, T, R> resultMapper) {
            super(actual);
            this.comparator = comparator;
            this.resultMapper = resultMapper;
        }

        @Override
        protected void updateResult(T newValue) {
            hasValue = true;
            if (min == null) {
                min = newValue;
            }
            if (max == null) {
                max = newValue;
            }
            max = comparator.compare(newValue, max) > 0 ? newValue : max;

            min = comparator.compare(newValue, min) > 0 ? min : newValue;

        }

        @Override
        protected R result() {
            if (!hasValue) {
                return null;
            }
            return resultMapper.apply(min, max);
        }

        @Override
        protected void reset() {
            hasValue = false;
            min = null;
            max = null;
        }
    }
}