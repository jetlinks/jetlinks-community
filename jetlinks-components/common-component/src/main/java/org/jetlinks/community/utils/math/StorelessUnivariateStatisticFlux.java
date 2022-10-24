package org.jetlinks.community.utils.math;

import lombok.Generated;
import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

class StorelessUnivariateStatisticFlux<T> extends org.jetlinks.community.utils.math.MonoFromFluxOperator<T, Double> {

    final Function<? super T, Double> mapping;

    final Supplier<StorelessUnivariateStatistic> statisticSupplier;

    public StorelessUnivariateStatisticFlux(Flux<? extends T> source,
                                            Function<? super T, Double> mapping,
                                            Supplier<StorelessUnivariateStatistic> statisticSupplier) {
        super(source);
        this.mapping = mapping;
        this.statisticSupplier = statisticSupplier;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super Double> coreSubscriber) {
        source.subscribe(new StorelessUnivariateSubscriber<>(coreSubscriber, statisticSupplier, mapping));
    }

    @Generated
    static final class StorelessUnivariateSubscriber<T> extends org.jetlinks.community.utils.math.MathSubscriber<T, Double> {

        final Function<? super T, ? extends Number> mapping;

        final StorelessUnivariateStatistic statistic;

        boolean hasValue;

        StorelessUnivariateSubscriber(CoreSubscriber<? super Double> actual,
                                      Supplier<StorelessUnivariateStatistic> statisticSupplier,
                                      Function<? super T, ? extends Number> mapping) {
            super(actual);
            this.mapping = mapping;
            this.statistic = statisticSupplier.get();
        }

        @Override
        protected void updateResult(T newValue) {
            double val = mapping.apply(newValue).doubleValue();
            hasValue = true;
            statistic.increment(val);
        }

        @Override
        protected Double result() {
            return hasValue ? statistic.getResult() : null;
        }

        @Override
        protected void reset() {
            statistic.clear();
            hasValue = false;
        }
    }
}
