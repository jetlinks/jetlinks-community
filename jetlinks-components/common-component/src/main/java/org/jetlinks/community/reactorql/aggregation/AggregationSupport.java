package org.jetlinks.community.reactorql.aggregation;

import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.dml.FunctionColumn;
import org.jetlinks.community.spi.Provider;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 聚合函数支持.
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
public interface AggregationSupport extends Function<Publisher<?>, Mono<?>> {

    Provider<AggregationSupport> supports = Provider.create(AggregationSupport.class);

    String getId();

    String getName();

    SqlFragments createSql(FunctionColumn column);


    static AggregationSupport getNow(String id) {
        return AggregationSupport.supports
            .get(id.toUpperCase())
            .orElseGet(() -> AggregationSupport.supports
                .get(id.toLowerCase())
                .orElseGet(() -> AggregationSupport.supports.getNow(id)));
    }
}
