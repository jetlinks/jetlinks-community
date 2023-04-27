package org.jetlinks.community.things.data.operations;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.Wrapper;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.community.things.data.*;
import org.jetlinks.community.timeseries.query.AggregationData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 查询操作,用于查询物相关数据
 *
 * @author zhouhao
 * @since 2.0
 */
public interface QueryOperations extends Wrapper {

    /**
     * 按条件查询每一个属性,通常用于同时查询多个属性值.
     *
     * <p>
     * 根据{@link QueryParamEntity#isPaging()}决定是否进行分页,如果要查询全部数据,需要设置{@link QueryParamEntity#setPaging(boolean)}为false.
     *
     * @param query    查询条件
     * @param property 指定要查询的属性,不指定则查询全部属性.
     * @return 属性数据
     */
    @Nonnull
    Flux<ThingPropertyDetail> queryEachProperty(@Nonnull QueryParamEntity query,
                                                @Nonnull String... property);


    /**
     * 查询属性数据,并返回属性数据列表,一条数据表示一个属性值.
     * <p>
     * 根据{@link QueryParamEntity#isPaging()}决定是否进行分页,如果要查询全部数据,需要设置{@link QueryParamEntity#setPaging(boolean)}为false.
     *
     * @param query    查询条件
     * @param property 指定要查询的属性,不指定则查询全部属性.
     * @return 属性数据
     */
    @Nonnull
    Flux<ThingPropertyDetail> queryProperty(@Nonnull QueryParamEntity query,
                                            @Nonnull String... property);

    /**
     * 分页查询属性数据,通常用于查询单个属性的历史列表.
     * <p>
     * 如果指定多个属性,不同的存储策略实现可能返回结果不同.
     *
     * @param query    查询条件
     * @param property 指定要查询的属性,不指定则查询全部属性.
     * @return 属性数据
     */
    @Nonnull
    Mono<PagerResult<ThingPropertyDetail>> queryPropertyPage(@Nonnull QueryParamEntity query,
                                                             @Nonnull String... property);

    /**
     * 聚合查询属性数据
     *
     * @param request    聚合请求
     * @param properties 属性聚合方式
     * @return 聚合查询结果
     */
    @Nonnull
    Flux<AggregationData> aggregationProperties(@Nonnull AggregationRequest request,
                                                @Nonnull PropertyAggregation... properties);


    /**
     * 查询设备日志,不返回分页结果
     *
     * @param query 查询条件
     * @return 查询结果
     */
    Flux<ThingMessageLog> queryMessageLog(@Nonnull QueryParamEntity query);

    /**
     * 分页查询设备日志
     *
     * @param query 查询条件
     * @return 查询结果
     */
    Mono<PagerResult<ThingMessageLog>> queryMessageLogPage(@Nonnull QueryParamEntity query);


    /**
     * 分页查询事件数据
     *
     * @param eventId 事件ID
     * @param query   查询条件
     * @param format  是否对数据进行格式化. {@link ThingEvent#putFormat(EventMetadata)}
     * @return 分页查询结果
     */
    @Nonnull
    Mono<PagerResult<ThingEvent>> queryEventPage(@Nonnull String eventId,
                                                 @Nonnull QueryParamEntity query,
                                                 boolean format);

    /**
     * 查询事件数据,不返回分页结果.
     * <p>
     * 根据{@link QueryParamEntity#isPaging()}决定是否进行分页,如果要查询全部数据,需要设置{@link QueryParamEntity#setPaging(boolean)}为false.
     *
     * @param eventId 事件ID
     * @param query   查询条件
     * @param format  是否对数据进行格式化. {@link ThingEvent#putFormat(EventMetadata)}
     * @return 事件数据
     */
    @Nonnull
    Flux<ThingEvent> queryEvent(@Nonnull String eventId,
                                @Nonnull QueryParamEntity query,
                                boolean format);

}
