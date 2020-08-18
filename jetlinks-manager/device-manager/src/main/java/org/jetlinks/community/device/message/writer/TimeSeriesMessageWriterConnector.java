package org.jetlinks.community.device.message.writer;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.device.enums.DeviceLogType;
import org.jetlinks.community.device.events.handler.ValueTypeTranslator;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.types.UnknownType;
import org.reactivestreams.Publisher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.function.Consumer;

/**
 * 用于将设备消息写入到时序数据库
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
@ConfigurationProperties(prefix = "jetlinks.device.log")
public class TimeSeriesMessageWriterConnector{
    public TimeSeriesManager timeSeriesManager;

    public DeviceRegistry registry;

    @Setter
    @Getter
    private Set<String> excludes = new HashSet<>();

    public TimeSeriesMessageWriterConnector(TimeSeriesManager timeSeriesManager, DeviceRegistry registry) {
        this.timeSeriesManager = timeSeriesManager;
        this.registry = registry;
    }

    @Subscribe(topics = "/device/**",id = "device-message-ts-writer")
    public Mono<Void> writeDeviceMessageToTs(DeviceMessage message){
        return commitDeviceMessage(message);
    }

    public Mono<Void> saveDeviceMessage(Publisher<DeviceMessage> message) {

        return Flux.from(message)
            .flatMap(this::convert)
            .groupBy(Tuple2::getT1)
            .flatMap(groups -> timeSeriesManager
                .getService(groups.key())
                .save(groups.map(Tuple2::getT2)))
            .then();
    }

    public Mono<Void> commitDeviceMessage(DeviceMessage message) {
        return this
            .convert(message)
            .flatMap(tp2 -> timeSeriesManager
                .getService(tp2.getT1())
                .commit(tp2.getT2()))
            .then();
    }

    protected Mono<Tuple2<String, TimeSeriesData>> createLog(String productId, DeviceMessage message, Consumer<DeviceOperationLogEntity> logEntityConsumer) {
        if (excludes.contains("*") || excludes.contains(message.getMessageType().name())) {
            return Mono.empty();
        }
        DeviceOperationLogEntity operationLog = new DeviceOperationLogEntity();
        operationLog.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
        operationLog.setDeviceId(message.getDeviceId());
        operationLog.setCreateTime(new Date(message.getTimestamp()));
        operationLog.setProductId(productId);
        operationLog.setType(DeviceLogType.of(message));

        if (null != logEntityConsumer) {
            logEntityConsumer.accept(operationLog);
        }
        return Mono.just(Tuples.of(DeviceTimeSeriesMetric.deviceLogMetricId(productId), TimeSeriesData.of(message.getTimestamp(), operationLog.toSimpleMap())));
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convert(DeviceMessage message) {
        Map<String, Object> headers = Optional.ofNullable(message.getHeaders()).orElse(Collections.emptyMap());

        String productId = (String) headers.getOrDefault("productId", "null");
        Consumer<DeviceOperationLogEntity> logEntityConsumer = null;
        List<Publisher<Tuple2<String, TimeSeriesData>>> all = new ArrayList<>();

        if (message instanceof EventMessage) {
            logEntityConsumer = log -> log.setContent(JSON.toJSONString(((EventMessage) message).getData()));
            all.add(convertEvent(productId, headers, ((EventMessage) message)));
        }
        //上报属性
        else if (message instanceof ReportPropertyMessage) {
            ReportPropertyMessage reply = (ReportPropertyMessage) message;
            Map<String, Object> properties = reply.getProperties();
            if (MapUtils.isNotEmpty(properties)) {
                logEntityConsumer = log -> log.setContent(properties);
                all.add(convertProperties(productId, headers, message, properties));
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
                all.add(convertProperties(productId, headers, message, properties));
            } else if (message instanceof WritePropertyMessageReply) {
                WritePropertyMessageReply reply = (WritePropertyMessageReply) message;
                Map<String, Object> properties = reply.getProperties();
                logEntityConsumer = log -> log.setContent(properties);
                all.add(convertProperties(productId, headers, message, properties));
            } else {
                logEntityConsumer = log -> log.setContent(message.toJson().toJSONString());
            }
        }
        //其他
        else {
            logEntityConsumer = log -> log.setContent(message.toJson().toJSONString());
        }
        all.add(createLog(productId, message, logEntityConsumer));
        return Flux.merge(all);
    }

    protected Mono<Tuple2<String, TimeSeriesData>> convertEvent(String productId, Map<String, Object> headers, EventMessage message) {

        return registry
            .getDevice(message.getDeviceId())
            .flatMap(device -> device.getMetadata()
                .map(metadata -> {
                    Object value = message.getData();
                    DataType dataType = metadata
                        .getEvent(message.getEvent())
                        .map(EventMetadata::getType)
                        .orElseGet(UnknownType::new);
                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", device.getDeviceId());
                    data.put("createTime", message.getTimestamp());
                    Object tempValue = ValueTypeTranslator.translator(value, dataType);
                    if (tempValue instanceof Map) {
                        data.putAll(((Map) tempValue));
                    } else {
                        data.put("value", tempValue);
                    }
                    return TimeSeriesData.of(message.getTimestamp(), data);
                }))
            .map(data -> Tuples.of(DeviceTimeSeriesMetric.deviceEventMetricId(productId, message.getEvent()), data));
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId,
                                                                     Map<String, Object> headers,
                                                                     DeviceMessage message,
                                                                     Map<String, Object> properties) {
        if (MapUtils.isEmpty(properties)) {
            return Flux.empty();
        }
        return registry
            .getDevice(message.getDeviceId())
            .flatMapMany(device -> device
                .getMetadata()
                .flatMapMany(metadata -> Flux
                    .fromIterable(properties.entrySet())
                    .map(entry -> {
                        DevicePropertiesEntity entity = DevicePropertiesEntity.builder()
                            .deviceId(device.getDeviceId())
                            .timestamp(message.getTimestamp())
                            .property(entry.getKey())
                            .propertyName(entry.getKey())
                            .orgId((String) headers.get("orgId"))
                            .productId(productId)
                            .build()
                            .withValue(metadata.getPropertyOrNull(entry.getKey()), entry.getValue());

                        return TimeSeriesData.of(message.getTimestamp(), entity.toMap());
                    })
                    .map(data -> Tuples.of(DeviceTimeSeriesMetric.devicePropertyMetricId(productId), data)))
            );
    }

}
