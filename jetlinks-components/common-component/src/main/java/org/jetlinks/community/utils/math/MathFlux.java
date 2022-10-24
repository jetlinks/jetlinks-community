package org.jetlinks.community.utils.math;

import lombok.AccessLevel;
import lombok.Generated;
import lombok.NoArgsConstructor;
import org.apache.commons.math3.stat.descriptive.moment.*;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.summary.SumOfSquares;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetlinks.reactor.ql.supports.agg.MapAggFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.reactor.ql.utils.CompareUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata.addGlobal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Generated
public abstract class MathFlux {

    static {

        addGlobal(new MapAggFeature("skewness", stream -> skewness(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));
        addGlobal(new MapAggFeature("kurtosis", stream -> kurtosis(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));
        addGlobal(new MapAggFeature("mean", stream -> mean(stream, val -> CastUtils.castNumber(val).doubleValue())));
        addGlobal(new MapAggFeature("variance", stream -> variance(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));
        addGlobal(new MapAggFeature("geo_mean", stream -> geometricMean(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));
        addGlobal(new MapAggFeature("sum_of_squ", stream -> sumOfSquares(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));
        addGlobal(new MapAggFeature("std_dev", stream -> standardDeviation(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));

        addGlobal(new MapAggFeature("slope", stream -> slope(stream, val -> CastUtils.castNumber(val).doubleValue())));

        addGlobal(new MapAggFeature("range", stream -> rangeNumber(stream.map(CastUtils::castNumber), Function.identity())));

        addGlobal(new MapAggFeature("median", stream -> median(stream, val -> CastUtils
            .castNumber(val)
            .doubleValue())));

    }

    public static void load() {
    }

    //偏度特征值
    public static <T> Mono<Double> skewness(Publisher<T> source,
                                            Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, Skewness::new);
    }

    //峰度特征
    public static <T> Mono<Double> kurtosis(Publisher<T> source,
                                            Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, Kurtosis::new);
    }

    //算术平均值
    public static <T> Mono<Double> mean(Publisher<T> source,
                                        Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, Mean::new);
    }

    //方差
    public static <T> Mono<Double> variance(Publisher<T> source,
                                            Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, Variance::new);
    }

    //几何平均数
    public static <T> Mono<Double> geometricMean(Publisher<T> source,
                                                 Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, GeometricMean::new);
    }

    //平方和
    public static <T> Mono<Double> sumOfSquares(Publisher<T> source,
                                                Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, SumOfSquares::new);
    }

    //标准差
    public static <T> Mono<Double> standardDeviation(Publisher<T> source,
                                                     Function<T, Double> mapping) {
        return new StorelessUnivariateStatisticFlux<>(Flux.from(source), mapping, StandardDeviation::new);
    }

    //斜度
    public static <T> Mono<Double> slope(Publisher<T> source,
                                         Function<T, Double> mapping) {
        return new org.jetlinks.community.utils.math.RegressionFlux<>(Flux.from(source), mapping, SimpleRegression::getSlope);
    }

    //中位数
    public static <T> Mono<Double> median(Publisher<T> source,
                                          Function<T, Double> mapping) {
        return new org.jetlinks.community.utils.math.UnivariateStatisticFlux<>(Flux.from(source), mapping, Median::new);
    }

    //极差
    public static <T, R> Mono<R> range(Publisher<T> source,
                                       Comparator<T> comparator,
                                       BiFunction<T, T, R> mapper) {
        return new org.jetlinks.community.utils.math.RangeFlux<>(Flux.from(source), comparator, mapper);
    }

    //数字极差
    public static <R> Mono<R> rangeNumber(Publisher<Number> source,
                                          Function<Number, R> mapper) {
        return range(Flux.from(source),
                     CompareUtils::compare,
                     (min, max) -> {
                         if (min instanceof Integer && max instanceof Integer) {
                             return mapper.apply(max.intValue() - min.intValue());
                         }
                         if (min instanceof Long && max instanceof Long) {
                             return mapper.apply(max.longValue() - min.longValue());
                         }
                         if (min instanceof Float && max instanceof Float) {
                             return mapper.apply(max.floatValue() - min.floatValue());
                         }
                         if (min instanceof BigDecimal && max instanceof BigDecimal) {
                             return mapper.apply(((BigDecimal) max).subtract(((BigDecimal) min)));
                         }
                         if (min instanceof BigInteger && max instanceof BigInteger) {
                             return mapper.apply(((BigInteger) max).subtract(((BigInteger) min)));
                         }
                         return mapper.apply(max.doubleValue() - min.doubleValue());
                     });
    }


}
