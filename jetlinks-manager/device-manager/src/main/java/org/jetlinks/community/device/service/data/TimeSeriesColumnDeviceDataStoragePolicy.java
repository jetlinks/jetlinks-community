package org.jetlinks.community.device.service.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.timeseries.query.*;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
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
import static org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric.devicePropertyMetricId;

@Component
public class TimeSeriesColumnDeviceDataStoragePolicy extends TimeSeriesDeviceDataStoragePolicy implements DeviceDataStoragePolicy {

    public TimeSeriesColumnDeviceDataStoragePolicy(DeviceRegistry deviceRegistry,
                                                   TimeSeriesManager timeSeriesManager,
                                                   DeviceDataStorageProperties properties) {
        super(deviceRegistry, timeSeriesManager, properties);
    }

    @Override
    public String getId() {
        return "default-column";
    }

    @Override
    public String getName() {
        return "默认-列式存储";
    }

    @Override
    public String getDescription() {
        return "每个设备的全部属性为一行数据.需要设备每次上报全部属性.";
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
                timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.properties(productId, metadata.getProperties())),
                timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.log(productId)))
            .then();
    }


    private Flux<DeviceProperty> queryEachDeviceProperty(String productId,
                                                         String deviceId,
                                                         Map<String, PropertyMetadata> property,
                                                         QueryParamEntity param) {


        //查询多个属性,分组聚合获取第一条数据
        return param
            .toQuery()
            .includes(property.keySet().toArray(new String[0]))
            .where("deviceId", deviceId)
            .execute(q -> timeSeriesManager.getService(getPropertyTimeSeriesMetric(productId)).query(q))
            .flatMap(data -> rowToProperty(data, property.values()));
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
            .flatMap(tp2 -> {
                    PropertyMetadata prop = tp2.getT2().getPropertyOrNull(property);

                    return param.toQuery()
                        .includes(property)
                        .execute(query -> timeSeriesManager
                            .getService(devicePropertyMetric(tp2.getT1().getId()))
                            .queryPager(query,
                                data -> DeviceProperty
                                    .of(data, data.get(property).orElse(0), prop)
                                    .property(property)
                            ));
                }
            );
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
                    Set<String> includes = new HashSet<>(Arrays.asList(property));
                    Map<String, PropertyMetadata> propertiesMap = tp2.getT2()
                        .getProperties()
                        .stream()
                        .filter(prop -> includes.size() > 0 && includes.contains(prop.getId()))
                        .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

                    return query
                        .toQuery()
                        .where("deviceId", deviceId)
                        .includes(property)
                        .execute(timeSeriesManager.getService(getPropertyTimeSeriesMetric(tp2.getT1().getId()))::query)
                        .flatMap(data -> Flux
                            .fromIterable(propertiesMap.entrySet())
                            .map(entry -> DeviceProperty.of(
                                data,
                                data.get(entry.getKey()).orElse(null),
                                entry.getValue()
                            ).property(entry.getKey()))
                        );
                }));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query) {

        return deviceRegistry
            .getDevice(deviceId)
            .flatMapMany(device -> Mono
                .zip(device.getProduct(), device.getMetadata())
                .flatMapMany(tp2 -> {

                    Map<String, PropertyMetadata> propertiesMap = tp2.getT2()
                        .getProperties()
                        .stream()
                        .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

                    return queryEachDeviceProperty(tp2.getT1().getId(), deviceId, propertiesMap, query);
                }));
    }


    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull DeviceDataService.AggregationRequest request,
                                                                @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern(request.getFormat());

        return AggregationQueryParam.of()
            .as(param -> {
                for (DeviceDataService.DevicePropertyAggregation property : properties) {
                    param.agg(property.getProperty(), property.getAlias(), property.getAgg());
                }
                return param;
            })
            .as(param -> {
                if (request.interval == null) {
                    return param;
                }
                return param.groupBy((Group) new TimeGroup(request.interval, "time", request.format));
            })
            .limit(request.limit * properties.length)
            .from(request.from)
            .to(request.to)
            .filter(request.filter)
            .execute(timeSeriesManager.getService(getPropertyTimeSeriesMetric(productId))::aggregation)
            .groupBy(agg -> agg.getString("time", ""))
            .flatMap(group -> group
                .map(AggregationData::asMap)
                .reduce((a, b) -> {
                    a.putAll(b);
                    return a;
                })
                .map(AggregationData::of))
            .sort(Comparator.<AggregationData, Date>comparing(agg -> DateTime.parse(agg.getString("time", ""), formatter).toDate()).reversed())
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
        return convertPropertiesForColumnPolicy(productId, message, properties);
    }

    protected Object convertPropertyValue(Object value, PropertyMetadata metadata) {
        if (value == null || metadata == null) {
            return value;
        }
        if (metadata instanceof Converter) {
            return ((Converter<?>) metadata).convert(value);
        }
        return value;
    }
}
