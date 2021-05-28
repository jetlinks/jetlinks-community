package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 设备数据存储策略
 *
 * @author zhouhao
 * @since 1.5
 */
public interface DeviceDataStoragePolicy {

    /**
     * @return 策略唯一标识
     */
    String getId();

    /**
     * @return 策略名称
     */
    String getName();

    /**
     * @return 说明
     */
    String getDescription();

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
     * 获取配置信息
     *
     * @return 配置信息
     */
    @Nonnull
    Mono<ConfigMetadata> getConfigMetadata();

    /**
     * 注册设备物模型
     *
     * @param productId 产品ID
     * @param metadata  模型
     * @return void
     */
    @Nonnull
    Mono<Void> registerMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata);

    /**
     * 获取设备最新属性,Map key为属性标识，值为属性值
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
     * 查询设备事件
     *
     * @param deviceId 设备ID
     * @param event    事件标识
     * @param query    查询条件
     * @return 设备事件数据
     */
    @Nonnull
    Flux<DeviceEvent> queryEvent(@Nonnull String deviceId,
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
    Mono<PagerResult<DeviceEvent>> queryEventPage(@Nonnull String deviceId,
                                                  @Nonnull String event,
                                                  @Nonnull QueryParamEntity query,
                                                  boolean format);


    /**
     * 查询所有设备属性
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 设备属性
     */
    @Nonnull
    Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                             @Nonnull QueryParamEntity query,
                                             @Nonnull String... property);

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
     * @param properties 指定聚合属性
     * @return 聚合查询结果
     */
    Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                         @Nonnull DeviceDataService.AggregationRequest request,
                                                         @Nonnull DeviceDataService.DevicePropertyAggregation... properties);

    /**
     * 根据设备ID聚合查询属性
     *
     * @param deviceId   设备ID
     * @param request    聚合请求
     * @param properties 指定聚合属性
     * @return 聚合查询结果
     */
    Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                        @Nonnull DeviceDataService.AggregationRequest request,
                                                        @Nonnull DeviceDataService.DevicePropertyAggregation... properties);

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
     * 分页查询设备日志
     *
     * @param deviceId 设备ID
     * @param query    查询条件
     * @return 查询结果
     */
    Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceMessageLog(@Nonnull String deviceId,
                                                                      @Nonnull QueryParamEntity query);


}
