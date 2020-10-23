package org.jetlinks.community.device.service.data;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.community.device.entity.DeviceEvent;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.enums.DeviceLogType;
import org.jetlinks.community.device.events.handler.ValueTypeTranslator;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

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
            .groupBy(Tuple2::getT1)
            .flatMap(group -> doSaveData(group.key(), group.map(Tuple2::getT2)))
            .then();
    }

    protected String createDataId(DeviceMessage message) {
        long ts = message.getTimestamp();
        return DigestUtils.md5Hex(String.join("_", message.getDeviceId(), String.valueOf(createUniqueNanoTime(ts))));
    }

    protected Mono<Tuple2<String, TimeSeriesData>> createDeviceMessageLog(String productId,
                                                                          DeviceMessage message,
                                                                          Consumer<DeviceOperationLogEntity> logEntityConsumer) {
        DeviceOperationLogEntity operationLog = new DeviceOperationLogEntity();
        operationLog.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
        operationLog.setDeviceId(message.getDeviceId());
        operationLog.setTimestamp(message.getTimestamp());
        operationLog.setCreateTime(System.currentTimeMillis());
        operationLog.setProductId(productId);
        operationLog.setType(DeviceLogType.of(message));

        if (null != logEntityConsumer) {
            logEntityConsumer.accept(operationLog);
        }
        message.getHeader("log").ifPresent(operationLog::setContent);
        return Mono.just(Tuples.of(DeviceTimeSeriesMetric.deviceLogMetricId(productId), TimeSeriesData.of(message.getTimestamp(), operationLog.toSimpleMap())));
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convertMessageToTimeSeriesData(DeviceMessage message) {
        String productId = (String) message.getHeader("productId").orElse("null");
        Consumer<DeviceOperationLogEntity> logEntityConsumer = null;
        List<Publisher<Tuple2<String, TimeSeriesData>>> all = new ArrayList<>();

        if (message instanceof EventMessage) {
            logEntityConsumer = log -> log.setContent(JSON.toJSONString(((EventMessage) message).getData()));
            all.add(convertEventMessageToTimeSeriesData(productId, ((EventMessage) message)));
        }
        //上报属性
        else if (message instanceof ReportPropertyMessage) {
            ReportPropertyMessage reply = (ReportPropertyMessage) message;
            Map<String, Object> properties = reply.getProperties();
            if (MapUtils.isNotEmpty(properties)) {
                logEntityConsumer = log -> log.setContent(properties);
                all.add(convertProperties(productId, message, properties));
            }
        }
        //消息回复
        else if (message instanceof DeviceMessageReply) {
            //失败的回复消息
            if (!((DeviceMessageReply) message).isSuccess()) {
                logEntityConsumer = log -> log.setContent(message.toString());
            } else if (message instanceof ReadPropertyMessageReply) {
                ReadPropertyMessageReply reply = (ReadPropertyMessageReply) message;
                Map<String, Object> properties = reply.getProperties();
                logEntityConsumer = log -> log.setContent(properties);
                all.add(convertProperties(productId, message, properties));
            } else if (message instanceof WritePropertyMessageReply) {
                WritePropertyMessageReply reply = (WritePropertyMessageReply) message;
                Map<String, Object> properties = reply.getProperties();
                logEntityConsumer = log -> log.setContent(properties);
                all.add(convertProperties(productId, message, properties));
            } else {
                logEntityConsumer = log -> log.setContent(message.toJson().toJSONString());
            }
        }
        //其他
        else {
            logEntityConsumer = log -> log.setContent(message.toJson().toJSONString());
        }
        //配置了记录日志
        if (properties.getLog().match(message.getMessageType())) {
            all.add(createDeviceMessageLog(productId, message, logEntityConsumer));
        }

        return Flux.merge(all);
    }

    protected Mono<Tuple2<String, TimeSeriesData>> convertEventMessageToTimeSeriesData(String productId, EventMessage message) {

        return deviceRegistry
            .getDevice(message.getDeviceId())
            .flatMap(device -> device.getMetadata()
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
                        data = new HashMap<>((int) ((size / 0.75) + 7));
                        data.putAll(mapValue);
                    } else {
                        data = new HashMap<>();
                        data.put("value", tempValue);
                    }
                    data.put("id", createDataId(message));
                    data.put("deviceId", device.getDeviceId());
                    data.put("createTime", System.currentTimeMillis());

                    return TimeSeriesData.of(message.getTimestamp(), data);
                }))
            .map(data -> Tuples.of(DeviceTimeSeriesMetric.deviceEventMetricId(productId, message.getEvent()), data));
    }


    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceMessageLog(@Nonnull String deviceId, @Nonnull QueryParamEntity entity) {
        return deviceRegistry
            .getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId))
            .flatMap(productId -> this
                .doQueryPager(DeviceTimeSeriesMetric.deviceLogMetricId(productId),
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
            .flatMapMany(tp2 -> query.toQuery()
                .where("deviceId", deviceId)
                .execute(param -> this
                    .doQuery(DeviceTimeSeriesMetric.deviceEventMetricId(tp2.getT1().getId(), event),
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
                    .doQueryPager(DeviceTimeSeriesMetric.deviceEventMetricId(tp2.getT1().getId(), event),
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
        if (metadata instanceof Converter) {
            return ((Converter<?>) metadata).convert(value);
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
                .map(metadata -> {
                    int size = properties.size();
                    String id;
                    //强制使用时间戳作为数据ID
                    if (message.getHeader(Headers.useTimestampAsId).orElse(false)) {
                        id = String.join("_", message.getDeviceId(), String.valueOf(message.getTimestamp()));
                    } else {
                        id = createDataId(message);
                    }
                    Map<String, Object> newData = new HashMap<>(size < 5 ? 16 : (int) ((size + 5) / 0.75D) + 1);
                    properties.forEach((k, v) -> newData.put(k, convertPropertyValue(v, metadata.getPropertyOrNull(k))));
                    newData.put("deviceId", message.getDeviceId());
                    newData.put("productId", productId);
                    newData.put("timestamp", message.getTimestamp());
                    newData.put("createTime", System.currentTimeMillis());
                    newData.put("id", DigestUtils.md5Hex(id));
                    return Tuples.of(getPropertyTimeSeriesMetric(productId), TimeSeriesData.of(message.getTimestamp(), newData));
                }));
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
                    .map(entry -> {
                        String id;
                        long ts = message.getTimestamp();
                        String property = entry.getT2().getKey();
                        //强制使用时间戳作为数据ID
                        if (message.getHeader(Headers.useTimestampAsId).orElse(false)) {
                            id = String.join("_", message.getDeviceId(), property, String.valueOf(message.getTimestamp()));
                        } else {
                            id = String.join("_", message.getDeviceId(), property, String.valueOf(createUniqueNanoTime(ts)));
                        }
                        DevicePropertiesEntity entity = DevicePropertiesEntity.builder()
                            .id(DigestUtils.md5Hex(id))
                            .deviceId(device.getDeviceId())
                            .timestamp(ts)
                            .property(property)
                            .productId(productId)
                            .createTime(System.currentTimeMillis())
                            .build()
                            .withValue(metadata.getPropertyOrNull(entry.getT2().getKey()), entry.getT2().getValue());

                        return TimeSeriesData.of(entity.getTimestamp(), entity.toMap());
                    })
                    .map(data -> Tuples.of(DeviceTimeSeriesMetric.devicePropertyMetricId(productId), data)))
            );
    }

    protected String getPropertyTimeSeriesMetric(String productId) {
        return DeviceTimeSeriesMetric.devicePropertyMetricId(productId);
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
