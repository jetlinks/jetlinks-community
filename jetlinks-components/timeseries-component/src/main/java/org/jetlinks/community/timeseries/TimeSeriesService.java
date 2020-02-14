package org.jetlinks.community.timeseries;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 时序数据处理服务
 *
 * @author zhouhao
 * @since 1.0
 */
public interface TimeSeriesService {

    /**
     * 查询时序数据
     *
     * @param queryParam 查询条件
     * @return 时序数据结果流
     */
    Flux<TimeSeriesData> query(QueryParam queryParam);

    /**
     * 查询数量
     *
     * @param queryParam 查询条件
     * @return 数量
     */
    Mono<Integer> count(QueryParam queryParam);

    /**
     * 分页查询
     *
     * @param queryParam 查询参数
     * @return 查询结果
     */
    default Mono<PagerResult<TimeSeriesData>> queryPager(QueryParam queryParam) {
        return queryPager(queryParam, Function.identity());
    }

    /**
     * 分页查询并转换数据
     *
     * @param queryParam 查询参数
     * @param mapper     转换规则
     * @param <T>        结果类型
     * @return 查询结果
     */
    default <T> Mono<PagerResult<T>> queryPager(QueryParam queryParam, Function<TimeSeriesData, T> mapper) {
        return Mono.zip(count(queryParam), query(queryParam).map(mapper).collectList(), PagerResult::of);
    }

    /**
     * 聚合查询
     * <pre>
     *
     *      AggregationQueryParam.of()
     *                 .sum("count")
     *                 .filter(query-> query.where("type","type1"))
     *                 .groupBy(Duration.ofHours(1),"MM-dd HH")
     *                 .limit(10)
     *                 .execute(service::aggregation)
     *
     * </pre>
     * @param queryParam 聚合查询条件
     * @return 查询结果数据流
     */
    Flux<AggregationData> aggregation(AggregationQueryParam queryParam);

    /**
     * 保存数据
     *
     * @param data 数据流
     * @return 保存结果, 不 {@link Mono#error(Throwable)} 则成功
     */
    Mono<Void> save(Publisher<TimeSeriesData> data);

    /**
     * 保存数据
     *
     * @param data 单个数据
     * @return 保存结果, 不 {@link Mono#error(Throwable)} 则成功
     */
    Mono<Void> save(TimeSeriesData data);

}
