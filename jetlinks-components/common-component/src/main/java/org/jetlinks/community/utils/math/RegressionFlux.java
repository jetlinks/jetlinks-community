package org.jetlinks.community.utils.math;

import lombok.Generated;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.function.Function;

class RegressionFlux<T, R> extends MonoFromFluxOperator<T, R> {

    private final Function<T, ? extends Number> numberMapper;

    private final Function<SimpleRegression, R> resultMapper;

    protected RegressionFlux(Flux<? extends T> source,
                             Function<T, ? extends Number> numberMapper,
                             Function<SimpleRegression, R> resultMapper) {
        super(source);
        this.numberMapper = numberMapper;
        this.resultMapper = resultMapper;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super R> coreSubscriber) {
        source.subscribe(new RegressionSubscriber<>(coreSubscriber, numberMapper, resultMapper));
    }

    @Generated
    static final class RegressionSubscriber<T, R> extends MathSubscriber<T, R> {

        private final Function<T, ? extends Number> numberMapper;

        private final Function<SimpleRegression, R> resultMapper;

        final SimpleRegression regression;

        boolean hasValue;

        int x = 0;

        RegressionSubscriber(CoreSubscriber<? super R> actual,
                             Function<T, ? extends Number> numberMapper,
                             Function<SimpleRegression, R> resultMapper) {
            super(actual);
            this.numberMapper = numberMapper;
            this.resultMapper = resultMapper;
            this.regression = new SimpleRegression();
        }

        @Override
        protected void updateResult(T newValue) {
            double y = numberMapper.apply(newValue).doubleValue();
            hasValue = true;
            regression.addData(++x, y);
        }

        @Override
        protected R result() {
            return resultMapper.apply(regression);
        }

        @Override
        protected void reset() {
            hasValue = false;
            regression.clear();
        }
    }
}
