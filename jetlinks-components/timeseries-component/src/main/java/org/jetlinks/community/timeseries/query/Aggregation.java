package org.jetlinks.community.timeseries.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;

import java.util.Comparator;
import java.util.function.Function;


@AllArgsConstructor
public enum Aggregation implements EnumDict<String> {

    MIN(numberFlux -> MathFlux.min(numberFlux, Comparator.comparing(Number::doubleValue)), null),
    MAX(numberFlux -> MathFlux.max(numberFlux, Comparator.comparing(Number::doubleValue)),null),
    AVG(numberFlux -> MathFlux.averageDouble(numberFlux, Number::doubleValue),null),
    SUM(numberFlux -> MathFlux.sumDouble(numberFlux, Number::doubleValue),0),
    COUNT(Flux::count, 0),
    FIRST(numberFlux -> numberFlux.take(1).single(),null),
    TOP(numberFlux -> numberFlux.take(1).single(),null),

    //去重计数
    DISTINCT_COUNT(flux -> flux.distinct().count(),0),
    NONE(numberFlux -> Reactors.ALWAYS_ZERO, 0);

    private final Function<Flux<Number>, Mono<? extends Number>> computer;
    @Getter
    private final Number defaultValue;

    public <S> Mono<? extends Number> compute(Flux<S> source, Function<S, Number> mapper) {

        return computer.apply(source.mapNotNull(mapper));
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getText() {
        return name();
    }

    @Override
    public boolean isWriteJSONObjectEnabled() {
        return false;
    }

}