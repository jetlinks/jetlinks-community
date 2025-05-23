/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.reactorql.aggregation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.dml.FunctionColumn;
import org.jetlinks.reactor.ql.supports.agg.MapAggFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;

import java.util.Comparator;
import java.util.function.Function;

import static org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata.addGlobal;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@AllArgsConstructor
public enum InternalAggregationSupports implements AggregationSupport {
    COUNT("总数", Flux::count, 0),
    //去重计数
    DISTINCT_COUNT("总数(去重)", flux -> flux.distinct().count(), 0) {
        @Override
        public SqlFragments createSql(FunctionColumn column) {
            return new BatchSqlFragments().addSql("count(distinct ", column.getColumn(), ")");
        }
    },
    MIN("最小值",
        numberFlux -> MathFlux.min(numberFlux.map(CastUtils::castNumber), Comparator.comparing(Number::doubleValue)), null),
    MAX("最大值", numberFlux -> MathFlux.max(numberFlux.map(CastUtils::castNumber), Comparator.comparing(Number::doubleValue)), null),
    AVG("平均值", numberFlux -> MathFlux.averageDouble(numberFlux.map(CastUtils::castNumber), Number::doubleValue), null),
    SUM("总和", numberFlux -> MathFlux.sumDouble(numberFlux.map(CastUtils::castNumber), Number::doubleValue), 0),

    FIRST("第一个值", numberFlux -> numberFlux.take(1).singleOrEmpty(), null),
    LAST("最后一个值", numberFlux -> numberFlux.takeLast(1).singleOrEmpty(), null),

//    MEDIAN("中位数", numberFlux -> Mono.empty(), null),//中位数
//    SPREAD("极差", numberFlux -> Mono.empty(), null),//差值
//    STDDEV("标准差", numberFlux -> Mono.empty(), null),//标准差
    ;

    static {
        for (InternalAggregationSupports value : values()) {
            addGlobal(new MapAggFeature(value.getId(), value::apply));
            AggregationSupport.supports.register(value.getId(), value);
        }
    }

    public static void register(){

    }

    @Getter
    private final String name;

    private final Function<Flux<?>, Mono<?>> computer;
    @Getter
    private final Object defaultValue;

    @Override
    public SqlFragments createSql(FunctionColumn column) {
        return new BatchSqlFragments()
            .addSql(name() + "(").addSql(column.getColumn()).addSql(")");
    }

    @Override
    public String getId() {
        return name();
    }

    @Override
    public Mono<?> apply(Publisher<?> publisher) {
        return computer.apply(Flux.from(publisher));
    }
}
