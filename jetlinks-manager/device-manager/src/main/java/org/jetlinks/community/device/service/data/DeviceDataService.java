package org.jetlinks.community.device.service.data;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.Interval;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 设备数据服务
 *
 * @author zhouhao
 * @since 1.5
 */
public interface DeviceDataService {


    ConfigKey<String> STORE_POLICY_CONFIG_KEY = ConfigKey.of("storePolicy", "存储策略", String.class);

    /**
     * 注册设备物模型信息
     *
     * @param productId 产品ID
     * @param metadata  物模型
     * @return void
     */
    Mono<Void> registerMetadata(@Nonnull String productId,
                                @Nonnull DeviceMetadata metadata);

    Mono<Void> reloadMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata);

    /**
     * 批量保存消息
     *
     * @param message 设备消息
     * @return void
     * @see this#saveDeviceMessage(Publisher)
     */
    @Nonnull
    default Mono<Void> saveDeviceMessage(@Nonnull Collection<DeviceMessage> message) {
        return saveDeviceMessage(Flux.fromIterable(message));
    }

    /**
     * 保存单个设备消息,为了提升性能,存储策略会对保存请求进行缓冲,达到一定条件后
     * 再进行批量写出,具体由不同对存储策略实现。
     * <p>
     * 如果保存失败,在这里不会得到错误信息.
     *
     * @param message 设备消息
     * @return void
     */
    @Nonnull
    Mono<Void> saveDeviceMessage(@Nonnull DeviceMessage message);

    /**
     * 批量保存设备消息,通常此操作会立即保存数据.如果失败也会立即得到错误信息.
     *
     * @param message 设备消息
     * @return void
     */
    @Nonnull
    Mono<Void> saveDeviceMessage(@Nonnull Publisher<DeviceMessage> message);

    /**
     * 获取设备每个属性,只取一个结果.
     *
     * @param deviceId   设备ID
     * @param properties 指定设备属性标识,如果不传,则返回全部属性.
     * @return 设备属性
     */
    @Nonnull
    Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                @Nonnull QueryParamEntity query,
                                                @Nonnull String... properties);


    /**
     * 查询设备每个属性,可指定通过{@link QueryParamEntity#setPageSize(int)} () }每个属性的数量.
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 设备属性
     */
    @Nonnull
    Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                             @Nonnull QueryParamEntity query,
                                             @Nonnull String... properties);

    /**
     * 查询指定的设备属性列表
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @param property 属性列表
     * @return 设备属性
     */
    @Nonnull
    Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                       @Nonnull QueryParamEntity query,
                                       @Nonnull String... property);

    /**
     * 根据产品ID聚合查询属性
     *
     * @param productId  产品ID
     * @param request    聚合请求
     * @param properties 指定聚合属性,不指定是聚合所有属性
     * @return 聚合查询结果
     */
    Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                         @Nonnull AggregationRequest request,
                                                         @Nonnull DevicePropertyAggregation... properties);

    /**
     * 根据设备ID聚合查询属性
     *
     * @param deviceId   设备ID
     * @param request    聚合请求
     * @param properties 指定聚合属性,不指定是聚合所有属性
     * @return 聚合查询结果
     */
    Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                        @Nonnull AggregationRequest request,
                                                        @Nonnull DevicePropertyAggregation... properties);


    /**
     * 分页查询属性
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 分页查询结果
     */
    @Nonnull
    Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                        @Nonnull String property,
                                                        @Nonnull QueryParamEntity query);


    /**
     * 分页查询属性
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 分页查询结果
     * @since 1.9
     */
    @Nonnull
    Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                        @Nonnull QueryParamEntity query,
                                                        @Nonnull String... property);

    /**
     * 分页查询设备日志
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 查询结果
     */
    Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceMessageLog(@Nonnull String deviceId,
                                                                      @Nonnull QueryParamEntity query);


    /**
     * 查询设备事件,如果设置里format为true，将根据物模型对数据进行{@link org.jetlinks.core.metadata.DataType#format(Object)}.
     * 并将format后对值添加_format后缀设置到结果中.例如:
     * <pre>
     *     {
     *         "value":26.5,
     *         "value_format":"26.5℃"
     *     }
     * </pre>
     * <p>
     * 如果类型是结构体({@link org.jetlinks.core.metadata.types.ObjectType})类型,
     * 则会把对整个数据进行格式化后合并到{@link DeviceEvent#putAll(Map)}
     *
     * @param deviceId 设备ID
     * @param event    事件标识
     * @param query    查询条件
     * @param format   是否对数据进行格式化
     * @return 设备事件数据
     * @see DeviceEvent#putFormat(EventMetadata)
     */
    @Nonnull
    Flux<DeviceEvent> queryEvent(@Nonnull String deviceId,
                                 @Nonnull String event,
                                 @Nonnull QueryParamEntity query,
                                 boolean format);

    /**
     * 分页查询设备事件数据
     *
     * @param deviceId 设备ID
     * @param event    事件ID
     * @param query    查询条件
     * @param format   是否对数据进行格式化
     * @return 分页查询结果
     */
    @Nonnull
    Mono<PagerResult<DeviceEvent>> queryEventPage(@Nonnull String deviceId,
                                                  @Nonnull String event,
                                                  @Nonnull QueryParamEntity query,
                                                  boolean format);

    /**
     * 分页查询设备事件
     *
     * @param deviceId 设备ID
     * @param event    事件标识
     * @param query    查询条件
     * @return 设备事件数据
     */
    @Nonnull
    default Mono<PagerResult<DeviceEvent>> queryEventPage(@Nonnull String deviceId,
                                                          @Nonnull String event,
                                                          @Nonnull QueryParamEntity query) {
        return queryEventPage(deviceId, event, query, false);
    }

    /**
     * 查询设备属性数据,但是不返回分页结果
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 查询结果
     */
    @Nonnull
    Flux<DeviceProperties> queryProperties(@Nonnull String deviceId,
                                           @Nonnull QueryParamEntity query);

    /**
     * 根据产品分页查询属性数据,一个属性为一列,仅支持部分存储策略
     *
     * @param productId 产品ID
     * @param query     查询条件
     * @return 查询结果
     */
    @Nonnull
    Mono<PagerResult<DeviceProperties>> queryPropertiesPageByProduct(@Nonnull String productId,
                                                                     @Nonnull QueryParamEntity query);


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    class DevicePropertyAggregation {
        @Schema(description = "属性ID")
        private String property; //要聚合对字段

        @Schema(description = "别名,默认和property一致")
        private String alias; //别名

        @Schema(description = "聚合方式,支持(count,sum,max,min,avg)", type = "string")
        private Aggregation agg; //聚合函数

        public String getAlias() {
            if (StringUtils.isEmpty(alias)) {
                return property;
            }
            return alias;
        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    class AggregationRequest {
        //时间间隔
        @Schema(description = "间隔,如: 1d", type = "string", defaultValue = "1d")
        Interval interval = Interval.ofDays(1);
        //时间格式
        @Schema(defaultValue = "时间格式,如:yyyy-MM-dd", description = "yyyy-MM-dd")
        String format = "yyyy-MM-dd";

        @Schema(description = "时间从,如: 2020-09-01 00:00:00,支持表达式: now-1d")
        Date from = new DateTime()
            .plusMonths(-1)
            .withHourOfDay(0)
            .withMinuteOfHour(0)
            .withSecondOfMinute(0)
            .toDate();

        @Schema(description = "时间到,如: 2020-09-30 00:00:00,支持表达式: now-1d")
        Date to = new DateTime()
            .withHourOfDay(23)
            .withMinuteOfHour(59)
            .withSecondOfMinute(59)
            .toDate();

        @Schema(description = "实例限制")
        int limit = 30;

        //过滤条件
        @Schema(description = "过滤条件")
        QueryParamEntity filter = QueryParamEntity.of();

        public AggregationRequest copy() {
            return new AggregationRequest(interval, format, from, to, limit, filter.clone());
        }

        @Hidden
        public void setQuery(QueryParamEntity filter) {
            setFilter(filter);
        }
    }
}
