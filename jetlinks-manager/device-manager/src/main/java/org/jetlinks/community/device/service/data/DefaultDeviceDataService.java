package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.Value;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 默认设备数据服务
 * <p>
 * 管理设备存储策略、提供数据查询和入库操作
 *
 * @author zhouhao
 */
@Component
public class DefaultDeviceDataService implements DeviceDataService {

    private final DeviceRegistry deviceRegistry;

    private final Map<String, DeviceDataStoragePolicy> policies = new ConcurrentHashMap<>();

    private final Mono<DeviceDataStoragePolicy> defaultPolicyMono;

    private final DeviceDataStorageProperties properties;

    public DefaultDeviceDataService(DeviceRegistry registry,
                                    DeviceDataStorageProperties storeProperties,
                                    ObjectProvider<DeviceDataStoragePolicy> policies) {
        this.deviceRegistry = registry;
        this.properties = storeProperties;
        for (DeviceDataStoragePolicy policy : policies) {
            this.policies.put(policy.getId(), policy);
        }
        defaultPolicyMono = Mono
            .fromSupplier(() -> this.policies.get(properties.getDefaultPolicy()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("存储策略[" + storeProperties.getDefaultPolicy() + "]不存在")));
    }

    @Override
    public Mono<Void> registerMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata) {
        return this
            .getStoreStrategy(productId)
            .flatMap(policy -> policy.registerMetadata(productId, metadata))
            .then();
    }

    /**
     * 通过产品ID 获取存储策略
     *
     * @param productId 产品ID
     * @return 存储策略
     */
    Mono<DeviceDataStoragePolicy> getStoreStrategy(String productId) {
        // 从注册中心获取产品操作接口
        // 从配置中获取产品的存储策略
        // 巧妙的双层switchIfEmpty 外层判断空配置 内层判断空策略
        return deviceRegistry
            .getProduct(productId)
            .flatMap(product -> product
                .getConfig("storePolicy")
                .map(Value::asString)
                .map(conf -> Mono
                    .justOrEmpty(policies.get(conf))
                    .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("存储策略[" + deviceRegistry + "]不存在")))
                ).switchIfEmpty(Mono.just(defaultPolicyMono))
                .flatMap(Function.identity()));
    }

    /**
     * 通过设备ID 获取存储策略
     *
     * @param deviceId 设备ID
     * @return 存储策略
     */
    Mono<DeviceDataStoragePolicy> getDeviceStrategy(String deviceId) {
        // 从注册中心获取设备操作接口
        // 转换成产品操作接口
        // 继而通过转换的产品ID获取存储策略
        return deviceRegistry.getDevice(deviceId)
            .flatMap(DeviceOperator::getProduct)
            .map(DeviceProductOperator::getId)
            .flatMap(this::getStoreStrategy);
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                       @Nonnull QueryParamEntity query,
                                                       @Nonnull String... properties) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryEachOneProperties(deviceId, query, properties));
    }


    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query,
                                                    @Nonnull String... properties) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryEachProperties(deviceId, query, properties));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                              @Nonnull QueryParamEntity query,
                                              @Nonnull String... property) {

        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryProperty(deviceId, query, property));
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull AggregationRequest request,
                                                                @Nonnull DevicePropertyAggregation... properties) {
        return this
            .getStoreStrategy(productId)
            .flatMapMany(strategy -> strategy.aggregationPropertiesByProduct(productId, request.copy(), properties));
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                               @Nonnull AggregationRequest request,
                                                               @Nonnull DevicePropertyAggregation... properties) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.aggregationPropertiesByDevice(deviceId, request.copy(), properties));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity query) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMap(strategy -> strategy.queryPropertyPage(deviceId, property, query))
            .defaultIfEmpty(PagerResult.empty());
    }

    @Override
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceMessageLog(@Nonnull String deviceId, @Nonnull QueryParamEntity query) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMap(strategy -> strategy.queryDeviceMessageLog(deviceId, query))
            .defaultIfEmpty(PagerResult.empty());
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
    @Override
    public Mono<Void> saveDeviceMessage(@Nonnull DeviceMessage message) {
        return this
            .getDeviceStrategy(message.getDeviceId())
            .flatMap(strategy -> strategy.saveDeviceMessage(message));
    }

    @Nonnull
    @Override
    public Mono<Void> saveDeviceMessage(@Nonnull Publisher<DeviceMessage> message) {
        return Flux
            .from(message)
            .groupBy(DeviceMessage::getDeviceId)
            .flatMap(group -> this
                .getDeviceStrategy(group.key())
                .flatMap(policy -> policy.saveDeviceMessage(group)))
            .then();
    }

    @Nonnull
    @Override
    public Flux<DeviceEvent> queryEvent(@Nonnull String deviceId,
                                        @Nonnull String event,
                                        @Nonnull QueryParamEntity query, boolean format) {

        return this
            .getDeviceStrategy(deviceId)
            .flatMapMany(strategy -> strategy.queryEvent(deviceId, event, query, format));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceEvent>> queryEventPage(@Nonnull String deviceId, @Nonnull String event, @Nonnull QueryParamEntity query, boolean format) {
        return this
            .getDeviceStrategy(deviceId)
            .flatMap(strategy -> strategy.queryEventPage(deviceId, event, query, format))
            .defaultIfEmpty(PagerResult.empty());
    }

}
