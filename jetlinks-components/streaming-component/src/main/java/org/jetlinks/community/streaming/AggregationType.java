package org.jetlinks.community.streaming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.reactor.ql.utils.CompareUtils;
import reactor.core.publisher.Flux;
import reactor.math.MathFlux;

import java.util.function.Function;

@AllArgsConstructor
@Dict("streaming-agg-type")
@Getter
public enum AggregationType implements EnumDict<String> {
    count("计数", flux -> flux.count().flux()),
    sum("求和", flux -> MathFlux.sumDouble(flux, CastUtils::castNumber).flux()),
    avg("平均值", flux -> MathFlux.averageDouble(flux, CastUtils::castNumber).flux()),
    max("最大值", flux -> MathFlux.max(flux, CompareUtils::compare).flux()),
    min("最小值", flux -> MathFlux.min(flux, CompareUtils::compare).flux()),

    first("最初值", flux -> flux.take(1)),
    last("最近值", flux -> flux.takeLast(1)),
    range("极差值", flux -> org.jetlinks.community.utils.math.MathFlux
        .rangeNumber(flux.map(CastUtils::castNumber), Function.identity())
        .flux()
    ),
    median("中位数", flux -> org.jetlinks.community.utils.math.MathFlux
        .median(flux, val -> CastUtils.castNumber(val).doubleValue())
        .flux()),

    geometricMean("几何平均值", flux -> org.jetlinks.community.utils.math.MathFlux
        .geometricMean(flux, val -> CastUtils.castNumber(val).doubleValue())
        .flux()),

    mean("算术平均值", flux -> org.jetlinks.community.utils.math.MathFlux
        .mean(flux, val -> CastUtils.castNumber(val).doubleValue())
        .flux()),

    variance("方差", flux -> org.jetlinks.community.utils.math.MathFlux
        .variance(flux, val -> CastUtils.castNumber(val).doubleValue())
        .flux()),

    dev("标准差", flux -> org.jetlinks.community.utils.math.MathFlux
        .standardDeviation(flux, val -> CastUtils.castNumber(val).doubleValue())
        .flux()),

    slope("倾斜率", flux -> org.jetlinks.community.utils.math.MathFlux
        .slope(flux, val -> CastUtils.castNumber(val).doubleValue())
        .flux());

    private final String text;
    private final Function<Flux<?>, Flux<?>> computer;

    @Override
    public String getValue() {
        return name();
    }

    public Flux<?> compute(Flux<?> source){
        return computer.apply(source);
    }
}