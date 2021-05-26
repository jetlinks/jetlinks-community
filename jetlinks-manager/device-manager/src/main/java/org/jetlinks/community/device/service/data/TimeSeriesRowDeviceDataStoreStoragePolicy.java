package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetadata;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric.devicePropertyMetric;

@Component
public class TimeSeriesRowDeviceDataStoreStoragePolicy extends TimeSeriesDeviceDataStoragePolicy implements DeviceDataStoragePolicy {

    public TimeSeriesRowDeviceDataStoreStoragePolicy(DeviceRegistry deviceRegistry,
                                                     TimeSeriesManager timeSeriesManager,
                                                     DeviceDataStorageProperties properties) {
        super(deviceRegistry, timeSeriesManager, properties);
    }

    @Override
    public String getId() {
        return "default-row";
    }

    @Override
    public String getName() {
        return "默认-行式存储";
    }

    @Override
    public String getDescription() {
        return "每个设备的每一个属性为一行数据.适合设备每次上报部分属性.";
    }

    @Nonnull
    @Override
    public Mono<ConfigMetadata> getConfigMetadata() {
        return Mono.empty();
    }

    @Nonnull
    @Override
    public Mono<Void> registerMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata) {
        return Flux
            .concat(Flux
                    .fromIterable(metadata.getEvents())
                    .flatMap(event -> timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.event(productId, event))),
                timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.properties(productId)),
                timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.log(productId)))
            .then();
    }

    private Flux<DeviceProperty> queryEachDeviceProperty(String productId,
                                                         String deviceId,
                                                         Map<String, PropertyMetadata> property,
                                                         QueryParamEntity param) {
        if (property.isEmpty()) {
            return Flux.empty();
        }
        //只查询一个属性
        if (property.size() == 1) {
            return param
                .toQuery()
                .where("deviceId", deviceId)
                .and("property", property.keySet().iterator().next())
                .execute(timeSeriesManager.getService(devicePropertyMetric(productId))::query)
                .map(data ->
                    DeviceProperty
                        .of(data, data.getString("property").map(property::get).orElse(null))
                        .deviceId(deviceId));
        }

        //查询多个属性,分组聚合获取第一条数据
        return timeSeriesManager
            .getService(devicePropertyMetric(productId))
            .aggregation(AggregationQueryParam
                .of()
                .agg("property", Aggregation.FIRST)
                .groupBy(new LimitGroup("property", "property", property.size() * 2)) //按property分组
                .limit(property.size())
                .filter(param)
                .filter(query -> query.where("deviceId", deviceId))
            ).map(data -> DeviceProperty
                .of(data, data.getString("property").map(property::get).orElse(null))
                .deviceId(deviceId));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                       @Nonnull QueryParamEntity query,
                                                       @Nonnull String... properties) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMapMany(device -> Mono
                .zip(device.getProduct(), device.getMetadata())
                .flatMapMany(tp2 -> {

                    Map<String, PropertyMetadata> propertiesMap = (properties.length == 0
                        ? tp2.getT2().getProperties().stream()
                        : Stream.of(properties).map(tp2.getT2()::getPropertyOrNull).filter(Objects::nonNull))
                        .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

                    return queryEachDeviceProperty(tp2.getT1().getId(), deviceId, propertiesMap, query.clone().doPaging(0, 1));
                }));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity param) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(device -> Mono.zip(device.getProduct(), device.getMetadata()))
            .flatMap(tp2 -> param.toQuery()
                .where("property", property)
                .and("deviceId", deviceId)
                .execute(query -> timeSeriesManager
                    .getService(devicePropertyMetric(tp2.getT1().getId()))
                    .queryPager(query, data -> DeviceProperty.of(data, tp2.getT2().getPropertyOrNull(property)))));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                              @Nonnull QueryParamEntity query,
                                              @Nonnull String... property) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMapMany(device -> Mono
                .zip(device.getProduct(), device.getMetadata())
                .flatMapMany(tp2 -> {

                    Map<String, PropertyMetadata> propertiesMap = tp2.getT2()
                        .getProperties()
                        .stream()
                        .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

                    return query.toQuery()
                        .where("deviceId", deviceId)
                        .when(property.length > 0, q -> q.in("property", Arrays.asList(property)))
                        .execute(timeSeriesManager
                            .getService(DeviceTimeSeriesMetric.devicePropertyMetricId(tp2.getT1().getId()))::query)
                        .map(data -> DeviceProperty.of(data, propertiesMap.get(data.getString("property", null))));
                }));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query,
                                                    @Nonnull String... property) {

        return getProductAndMetadataByDevice(deviceId)
            .flatMapMany(tp2 -> {

                Map<String, PropertyMetadata> propertiesMap = getPropertyMetadata(tp2.getT2(), property)
                    .stream()
                    .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));
                if (propertiesMap.isEmpty()) {
                    return Flux.empty();
                }
                return timeSeriesManager
                    .getService(devicePropertyMetric(tp2.getT1().getId()))
                    .aggregation(AggregationQueryParam
                                     .of()
                                     .agg(new LimitAggregationColumn("property", "property", Aggregation.TOP, query.getPageSize()))
                                     .groupBy(new LimitGroup("property", "property", propertiesMap.size() * 2)) //按property分组
                                     .filter(query)
                                     .filter(q -> q.where("deviceId", deviceId).in("property", propertiesMap.keySet()))
                    ).map(data -> DeviceProperty
                        .of(data, data.getString("property").map(propertiesMap::get).orElse(null))
                        .deviceId(deviceId));
            });
    }

    protected String getTimeSeriesMetric(String productId) {
        return DeviceTimeSeriesMetric.devicePropertyMetricId(productId);
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull DeviceDataService.AggregationRequest request,
                                                                @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        //只聚合一个属性时
        if (properties.length == 1) {
            return AggregationQueryParam.of()
                .agg("numberValue", properties[0].getAlias(), properties[0].getAgg())
                .groupBy(request.interval, request.format)
                .limit(request.limit)
                .from(request.from)
                .to(request.to)
                .filter(request.filter)
                .filter(query -> query.where("property", properties[0].getProperty()))
                .execute(timeSeriesManager.getService(getTimeSeriesMetric(productId))::aggregation)
                .doOnNext(agg -> agg.values().remove("_time"));
        }

        Map<String, String> propertyAlias = Arrays.stream(properties)
            .collect(Collectors.toMap(DeviceDataService.DevicePropertyAggregation::getAlias, DeviceDataService.DevicePropertyAggregation::getProperty));

        return AggregationQueryParam.of()
            .as(param -> {
                Arrays.stream(properties)
                    .forEach(agg -> param.agg("numberValue", "value_" + agg.getAlias(), agg.getAgg()));
                return param;
            })
            .groupBy((Group) new TimeGroup(request.interval, "time", request.format))
            .groupBy(new LimitGroup("property", "property", properties.length))
            .limit(request.limit * properties.length)
            .from(request.from)
            .to(request.to)
            .filter(request.filter)
            .filter(query -> query.where().in("property", propertyAlias.values()))
            //执行查询
            .execute(timeSeriesManager.getService(getTimeSeriesMetric(productId))::aggregation)
            //按时间分组,然后将返回的结果合并起来
            .groupBy(agg -> agg.getString("time", ""),Integer.MAX_VALUE)
            .flatMap(group ->
                {
                    String time = group.key();
                    return group
                        //按属性分组
                        .groupBy(agg -> agg.getString("property", ""),Integer.MAX_VALUE)
                        .flatMap(propsGroup -> {
                            String property = propsGroup.key();
                            return propsGroup
                                .<Map<String, Object>>reduceWith(HashMap::new, (a, b) -> {
                                    a.putIfAbsent("time", time);
                                    a.putIfAbsent("_time", b.get("_time").orElseGet(Date::new));
                                    b.get("value_" + property).ifPresent(v -> a.put(property, v));
                                    return a;
                                });
                        })
                        .<Map<String, Object>>reduceWith(HashMap::new, (a, b) -> {
                            a.putAll(b);
                            return a;
                        });
                }
            )
            .map(map -> {
                map.remove("");
                propertyAlias
                    .keySet()
                    .forEach(key -> map.putIfAbsent(key, 0));
                return AggregationData.of(map);
            })
            .sort(Comparator.<AggregationData, Date>comparing(agg -> CastUtils.castDate(agg.values().get("_time"))).reversed())
            .doOnNext(agg -> agg.values().remove("_time"))
            ;
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                               @Nonnull DeviceDataService.AggregationRequest request,
                                                               @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {

        request.filter.and("deviceId", "eq", deviceId);

        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getProduct)
            .flatMapMany(product -> aggregationPropertiesByProduct(product.getId(), request, properties))
            .doOnNext(agg -> agg.values().remove("_time"));
    }

    @Override
    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId, DeviceMessage message, Map<String, Object> properties) {
        return convertPropertiesForRowPolicy(productId, message, properties);
    }
}
