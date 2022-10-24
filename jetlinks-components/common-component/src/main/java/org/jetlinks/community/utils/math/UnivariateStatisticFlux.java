package org.jetlinks.community.utils.math;

import lombok.Generated;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

class UnivariateStatisticFlux<T> extends MonoFromFluxOperator<T, Double> {

    final Function<? super T, Double> mapping;

    final Supplier<UnivariateStatistic> statisticSupplier;

    public UnivariateStatisticFlux(Flux<? extends T> source,
                                   Function<? super T, Double> mapping,
                                   Supplier<UnivariateStatistic> statisticSupplier) {
        super(source);
        this.mapping = mapping;
        this.statisticSupplier = statisticSupplier;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super Double> coreSubscriber) {
        source.subscribe(new UnivariateStatisticSubscriber<>(coreSubscriber, statisticSupplier, mapping));
    }

    @Generated
    static final class UnivariateStatisticSubscriber<T> extends MathSubscriber<T, Double> {

        final Function<? super T, ? extends Number> mapping;

        final UnivariateStatistic statistic;

        boolean hasValue;

        final DoubleArray numbers;

        UnivariateStatisticSubscriber(CoreSubscriber<? super Double> actual,
                                      Supplier<UnivariateStatistic> statisticSupplier,
                                      Function<? super T, ? extends Number> mapping) {
            super(actual);
            this.mapping = mapping;
            this.numbers=new ResizableDoubleArray();
            this.statistic = statisticSupplier.get();
        }

        @Override
        protected void updateResult(T newValue) {
            double val = mapping.apply(newValue).doubleValue();
            hasValue = true;
            numbers.addElement(val);
        }

        @Override
        protected Double result() {
            return hasValue ? statistic.evaluate(numbers.getElements()) : null;
        }

        @Override
        protected void reset() {
            hasValue = false;
            this.numbers.clear();
        }
    }
}
