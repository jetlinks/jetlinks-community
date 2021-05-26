package org.jetlinks.community.device.service.data;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceLogMessage;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceLogType;
import org.jetlinks.community.device.events.handler.ValueTypeTranslator;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.core.utils.DeviceMessageTracer;
import org.jetlinks.core.utils.TimestampUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jetlinks.community.device.service.data.StorageConstants.propertyIsIgnoreStorage;
import static org.jetlinks.community.device.service.data.StorageConstants.propertyIsJsonStringStorage;
import static org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric.*;

/**
 * 抽象设备数据数据存储,实现一些通用的逻辑
 *
 * @author zhouhao
 * @since 1.5.0
 */
public abstract class AbstractDeviceDataStoragePolicy implements DeviceDataStoragePolicy {

    protected DeviceRegistry deviceRegistry;

    protected DeviceDataStorageProperties properties;

    public AbstractDeviceDataStoragePolicy(DeviceRegistry registry,
                                           DeviceDataStorageProperties properties) {
        this.deviceRegistry = registry;
        this.properties = properties;
    }

    /**
     * 执行保存单个数据
     *
     * @param metric 指标ID
     * @param data   数据
     * @return void
     */
    protected abstract Mono<Void> doSaveData(String metric, TimeSeriesData data);

    /**
     * 执行保存批量数据
     *
     * @param metric 指标ID
     * @param data   数据
     * @return void
     */
    protected abstract Mono<Void> doSaveData(String metric, Flux<TimeSeriesData> data);

    /**
     * @param productId  产品ID
     * @param message    原始消息
     * @param properties 属性
     * @return 数据集合
     * @see this#convertPropertiesForColumnPolicy(String, DeviceMessage, Map)
     * @see this#convertPropertiesForRowPolicy(String, DeviceMessage, Map)
     */
    protected abstract Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId,
                                                                              DeviceMessage message,
                                                                              Map<String, Object> properties);

    protected abstract <T> Flux<T> doQuery(String metric,
                                           QueryParamEntity paramEntity,
                                           Function<TimeSeriesData, T> mapper);

    protected abstract <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                             QueryParamEntity paramEntity,
                                                             Function<TimeSeriesData, T> mapper);


    @Nonnull
    @Override
    public Mono<Void> saveDeviceMessage(@Nonnull DeviceMessage message) {
        return this
            .convertMessageToTimeSeriesData(message)
            .flatMap(tp2 -> doSaveData(tp2.getT1(), tp2.getT2()))
            .then();
    }

    @Nonnull
    @Override
    public Mono<Void> saveDeviceMessage(@Nonnull Publisher<DeviceMessage> message) {
        return Flux.from(message)
                   .flatMap(this::convertMessageToTimeSeriesData)
                   .groupBy(Tuple2::getT1, Integer.MAX_VALUE)
                   .flatMap(group -> doSaveData(group.key(), group.map(Tuple2::getT2)))
                   .then();
    }

    protected String createDataId(DeviceMessage message) {
        long ts = message.getTimestamp();
        return DigestUtils.md5Hex(String.join("_", message.getDeviceId(), String.valueOf(createUniqueNanoTime(ts))));
    }

    protected Mono<Tuple2<String, TimeSeriesData>> createDeviceMessageLog(String productId,
                                                                          DeviceMessage message,
                                                                          BiConsumer<DeviceMessage, DeviceOperationLogEntity> logEntityConsumer) {
        DeviceOperationLogEntity operationLog = new DeviceOperationLogEntity();
        operationLog.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
        operationLog.setDeviceId(message.getDeviceId());
        operationLog.setTimestamp(TimestampUtils.toMillis(message.getTimestamp()));
        operationLog.setCreateTime(System.currentTimeMillis());
        operationLog.setProductId(productId);
        operationLog.setMessageId(message.getMessageId());
        operationLog.setType(DeviceLogType.of(message));

        if (null != logEntityConsumer) {
            logEntityConsumer.accept(message, operationLog);
        }
        message.getHeader("log").ifPresent(operationLog::setContent);
        return Mono.just(Tuples.of(deviceLogMetricId(productId), TimeSeriesData.of(message.getTimestamp(), operationLog
            .toSimpleMap())));
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convertMessageToTimeSeriesData(DeviceMessage message) {
        boolean ignoreStorage = message.getHeaderOrDefault(Headers.ignoreStorage);
        boolean ignoreLog = message.getHeaderOrDefault(Headers.ignoreLog);
        if (ignoreStorage && ignoreLog) {
            return Flux.empty();
        }
        DeviceMessageTracer.trace(message, "save.before");
        String productId = (String) message.getHeader("productId").orElse("null");
        BiConsumer<DeviceMessage, DeviceOperationLogEntity> logEntityConsumer = null;
        List<Publisher<Tuple2<String, TimeSeriesData>>> all = new ArrayList<>(2);

        //没有忽略数据存储
        if (!ignoreStorage) {
            //事件上报
            if (message instanceof EventMessage) {
                all.add(convertEventMessageToTimeSeriesData(productId, ((EventMessage) message)));
            } else {
                //属性相关
                Map<String, Object> properties = DeviceMessageUtils
                    .tryGetProperties(message)
                    .orElseGet(Collections::emptyMap);
                if (MapUtils.isNotEmpty(properties)) {
                    all.add(convertProperties(productId, message, properties));
                }
            }
        }
        //日志
        if (message instanceof DeviceLogMessage) {
            logEntityConsumer = (msg, log) -> log.setContent(((DeviceLogMessage) msg).getLog());
        }
        //配置了记录日志,并且消息头里没有标记忽略日志
        if (properties.getLog().match(message.getMessageType())
            && !ignoreLog) {
            if (logEntityConsumer == null) {
                logEntityConsumer = (msg, log) -> log.setContent(msg.toJson());
            }
            all.add(createDeviceMessageLog(productId, message, logEntityConsumer));
        }

        return Flux.merge(all);
    }

    protected Mono<Tuple2<String, TimeSeriesData>> convertEventMessageToTimeSeriesData(String productId, EventMessage message) {

        return deviceRegistry
            .getDevice(message.getDeviceId())
            .flatMap(device -> device
                .getMetadata()
                .map(metadata -> {
                    Object value = message.getData();
                    DataType dataType = metadata
                        .getEvent(message.getEvent())
                        .map(EventMetadata::getType)
                        .orElseGet(UnknownType::new);
                    Object tempValue = ValueTypeTranslator.translator(value, dataType);
                    Map<String, Object> data;
                    if (tempValue instanceof Map) {
                        @SuppressWarnings("all")
                        Map<String, Object> mapValue = ((Map) tempValue);
                        int size = mapValue.size();
                        data = newMap(size);
                        data.putAll(mapValue);
                    } else {
                        data = newMap(16);
                        data.put("value", tempValue);
                    }
                    data.put("id", createDataId(message));
                    data.put("deviceId", device.getDeviceId());
                    data.put("createTime", System.currentTimeMillis());

                    return TimeSeriesData.of(TimestampUtils.toMillis(message.getTimestamp()), data);
                }))
            .map(data -> Tuples.of(deviceEventMetricId(productId, message.getEvent()), data));
    }


    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceMessageLog(@Nonnull String deviceId, @Nonnull QueryParamEntity entity) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId))
            .flatMap(productId -> this
                .doQueryPager(deviceLogMetricId(productId),
                              entity.and("deviceId", TermType.eq, deviceId),
                              data -> data.as(DeviceOperationLogEntity.class)
                ))
            .defaultIfEmpty(PagerResult.empty());
    }


    @Nonnull
    @Override
    public Flux<DeviceEvent> queryEvent(@Nonnull String deviceId,
                                        @Nonnull String event,
                                        @Nonnull QueryParamEntity query,
                                        boolean format) {

        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(device -> Mono.zip(device.getProduct(), device.getMetadata()))
            .flatMapMany(tp2 -> query
                .toQuery()
                .where("deviceId", deviceId)
                .execute(param -> this
                    .doQuery(deviceEventMetricId(tp2.getT1().getId(), event),
                             param,
                             data -> {
                                 DeviceEvent deviceEvent = new DeviceEvent(data.values());
                                 if (format) {
                                     deviceEvent.putFormat(tp2.getT2().getEventOrNull(event));
                                 }
                                 deviceEvent.putIfAbsent("timestamp", data.getTimestamp());
                                 return deviceEvent;
                             })));
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceEvent>> queryEventPage(@Nonnull String deviceId,
                                                         @Nonnull String event,
                                                         @Nonnull QueryParamEntity query,
                                                         boolean format) {

        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(device -> Mono.zip(device.getProduct(), device.getMetadata()))
            .flatMap(tp2 -> query.toQuery()
                                 .where("deviceId", deviceId)
                                 .execute(param -> this
                                     .doQueryPager(deviceEventMetricId(tp2.getT1().getId(), event),
                                                   param,
                                                   data -> {
                                                       DeviceEvent deviceEvent = new DeviceEvent(data.values());
                                                       if (format) {
                                                           deviceEvent.putFormat(tp2.getT2().getEventOrNull(event));
                                                       }
                                                       deviceEvent.putIfAbsent("timestamp", data.getTimestamp());
                                                       return deviceEvent;
                                                   }))
            );
    }

    protected Flux<DeviceProperty> rowToProperty(TimeSeriesData row, Collection<PropertyMetadata> properties) {
        return Flux
            .fromIterable(properties)
            .filter(prop -> row.get(prop.getId()).isPresent())
            .map(property -> DeviceProperty.of(
                row,
                row.get(property.getId()).orElse(0),
                property
            ).property(property.getId()));
    }

    protected Object convertPropertyValue(Object value, PropertyMetadata metadata) {
        if (value == null || metadata == null) {
            return value;
        }
        //使用json字符串来存储
        if (propertyIsJsonStringStorage(metadata)) {
            return value instanceof String ? String.valueOf(value) : JSON.toJSONString(value);
        }
        if (metadata.getValueType() instanceof Converter) {
            return ((Converter<?>) metadata.getValueType()).convert(value);
        }
        return value;
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convertPropertiesForColumnPolicy(String productId,
                                                                                    DeviceMessage message,
                                                                                    Map<String, Object> properties) {
        if (MapUtils.isEmpty(properties)) {
            return Flux.empty();
        }
        return this
            .deviceRegistry
            .getDevice(message.getDeviceId())
            .flatMapMany(device -> device
                .getMetadata()
                .flatMap(metadata -> {
                    int size = properties.size();
                    String id;
                    //强制使用时间戳作为数据ID
                    if (message.getHeader(Headers.useTimestampAsId).orElse(false)) {
                        id = String.join("_", message.getDeviceId(), String.valueOf(message.getTimestamp()));
                    } else {
                        id = createDataId(message);
                    }
                    Mono<Map<String, Object>> dataSupplier;

                    int metaSize = metadata.getProperties().size();
                    //标记了是部分属性
                    if (message.getHeader(Headers.partialProperties).orElse(false)) {
                        dataSupplier = this
                            .queryEachOneProperties(message.getDeviceId(), QueryParamEntity.of())
                            .collectMap(DeviceProperty::getProperty, DeviceProperty::getValue, () -> newMap(metaSize + 5));
                    } else {
                        dataSupplier = Mono.just(newMap(size));
                    }
                    return dataSupplier
                        .flatMap(newData -> {
                            //转换属性数据
                            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                                PropertyMetadata propertyMetadata = metadata.getPropertyOrNull(entry.getKey());
                                //没有配置物模型或者忽略了存储
                                if (propertyMetadata == null || propertyIsIgnoreStorage(propertyMetadata)) {
                                    continue;
                                }
                                Object value = convertPropertyValue(entry.getValue(), propertyMetadata);
                                if (null != value) {
                                    newData.put(entry.getKey(), value);
                                }
                            }
                            //没有属性值,可能全部都配置了不存储
                            if (newData.isEmpty()) {
                                return Mono.empty();
                            }
                            newData.put("deviceId", message.getDeviceId());
                            newData.put("productId", productId);
                            newData.put("timestamp", TimestampUtils.toMillis(message.getTimestamp()));
                            newData.put("createTime", System.currentTimeMillis());
                            newData.put("id", DigestUtils.md5Hex(id));
                            return Mono.just(
                                Tuples.of(getPropertyTimeSeriesMetric(productId), TimeSeriesData.of(message.getTimestamp(), newData))
                            );
                        });
                }));
    }

    private Map<String, Object> newMap(int size) {
        return Maps.newHashMapWithExpectedSize(size);
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convertPropertiesForRowPolicy(String productId,
                                                                                 DeviceMessage message,
                                                                                 Map<String, Object> properties) {
        if (MapUtils.isEmpty(properties)) {
            return Flux.empty();
        }
        return this
            .deviceRegistry
            .getDevice(message.getDeviceId())
            .flatMapMany(device -> device
                .getMetadata()
                .flatMapMany(metadata -> Flux
                    .fromIterable(properties.entrySet())
                    .index()
                    .flatMap(entry -> {
                        String id;
                        long ts = message.getTimestamp();
                        String property = entry.getT2().getKey();
                        //忽略存在没有的属性和忽略存储的属性
                        PropertyMetadata propertyMetadata = metadata.getPropertyOrNull(property);
                        if (propertyMetadata == null || propertyIsIgnoreStorage(propertyMetadata)) {
                            return Mono.empty();
                        }
                        //强制使用时间戳作为数据ID
                        if (message.getHeader(Headers.useTimestampAsId).orElse(false)) {
                            id = String.join("_", message.getDeviceId(), property, String.valueOf(message.getTimestamp()));
                        } else {
                            id = String.join("_", message.getDeviceId(), property, String.valueOf(createUniqueNanoTime(ts)));
                        }
                        return Mono
                            .just(TimeSeriesData.of(ts, this
                                .createRowPropertyData(id,
                                                       TimestampUtils.toMillis(ts),
                                                       device.getDeviceId(),
                                                       propertyMetadata,
                                                       entry.getT2().getValue()))
                            );
                    })
                    .map(data -> Tuples.of(devicePropertyMetricId(productId), data)))
            );
    }

    protected Map<String, Object> createRowPropertyData(String id,
                                                        long timestamp,
                                                        String deviceId,
                                                        PropertyMetadata property,
                                                        Object value) {
        Map<String, Object> propertyData = newMap(24);
        propertyData.put("id", DigestUtils.md5Hex(id));
        propertyData.put("deviceId", deviceId);
        propertyData.put("timestamp", timestamp);
        propertyData.put("property", property.getId());
        propertyData.put("createTime", System.currentTimeMillis());

        fillRowPropertyValue(propertyData, property, value);
        return propertyData;
    }

    protected void fillRowPropertyValue(Map<String, Object> target, PropertyMetadata property, Object value) {
        if (value == null) {
            return;
        }
        if (property == null) {
            if (value instanceof Number) {
                target.put("numberValue", value);
            } else if (value instanceof Date) {
                target.put("timeValue", value);
            }
            target.put("value", String.valueOf(value));
            return;
        }
        DataType type = property.getValueType();
        target.put("type", type.getId());
        String convertedValue;
        if (type instanceof NumberType) {
            NumberType<?> numberType = (NumberType<?>) type;
            Number number = numberType.convertNumber(value);
            if (number == null) {
                throw new UnsupportedOperationException("无法将" + value + "转为" + type.getId());
            }
            convertedValue = String.valueOf(number);
            target.put("numberValue", number);
        } else if (type instanceof DateTimeType) {
            DateTimeType dateTimeType = (DateTimeType) type;
            convertedValue = String.valueOf(value);
            target.put("timeValue", dateTimeType.convert(convertedValue));
        } else if (propertyIsJsonStringStorage(property)) {
            //使用json字符来存储
            convertedValue = value instanceof String
                ? String.valueOf(value)
                : JSON.toJSONString(value);

        } else if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            Object val = objectType.convert(value);
            convertedValue = JSON.toJSONString(val);
            target.put("objectValue", val);
        } else if (type instanceof ArrayType) {
            ArrayType objectType = (ArrayType) type;
            Object val = objectType.convert(value);
            convertedValue = JSON.toJSONString(val);
            target.put("arrayValue", val);
        } else if (type instanceof GeoType) {
            GeoType geoType = (GeoType) type;
            GeoPoint val = geoType.convert(value);
            convertedValue = String.valueOf(val);
            target.put("geoValue", val);
        } else {
            convertedValue = String.valueOf(value);
        }
        target.put("value", convertedValue);
    }

    protected String getPropertyTimeSeriesMetric(String productId) {
        return devicePropertyMetricId(productId);
    }

    protected Mono<Tuple2<DeviceProductOperator, DeviceMetadata>> getProductAndMetadataByDevice(String deviceId) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(device -> Mono.zip(device.getProduct(), device.getMetadata()));
    }

    protected Mono<Tuple2<DeviceProductOperator, DeviceMetadata>> getProductAndMetadataByProduct(String productId) {
        return deviceRegistry
            .getProduct(productId)
            .flatMap(product -> Mono.zip(Mono.just(product), product.getMetadata()));
    }

    protected List<PropertyMetadata> getPropertyMetadata(DeviceMetadata metadata, String... properties) {
        if (properties == null || properties.length == 0) {
            return metadata.getProperties();
        }
        if (properties.length == 1) {
            return metadata.getProperty(properties[0])
                           .map(Arrays::asList)
                           .orElseGet(Collections::emptyList);
        }
        Set<String> ids = new HashSet<>(Arrays.asList(properties));
        return metadata
            .getProperties()
            .stream()
            .filter(prop -> ids.isEmpty() || ids.contains(prop.getId()))
            .collect(Collectors.toList());
    }



    private final AtomicInteger nanoInc = new AtomicInteger();

    //将毫秒转为纳秒，努力让数据不重复
    protected long createUniqueNanoTime(long millis) {
        long nano = TimeUnit.MILLISECONDS.toNanos(millis);

        int inc = nanoInc.incrementAndGet();

        if (inc >= 99990) {
            nanoInc.set(inc = 1);
        }

        return nano + inc;
    }

}
