package org.jetlinks.community.device.message.writer;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.enums.DeviceLogType;
import org.jetlinks.community.device.events.handler.ValueTypeTranslator;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.*;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * 用于将设备消息写入到时序数据库
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class TimeSeriesMessageWriterConnector
    implements MessageConnector,
    MessageConnection,
    MessageSubscriber {

    public TimeSeriesManager timeSeriesManager;

    public DeviceRegistry registry;

    public TimeSeriesMessageWriterConnector(TimeSeriesManager timeSeriesManager, DeviceRegistry registry) {
        this.timeSeriesManager = timeSeriesManager;
        this.registry = registry;
    }

    @Nonnull
    @Override
    public String getId() {
        return "device-message-ts-writer";
    }

    @Override
    public String getName() {
        return "写入设备消息到时序数据库";
    }

    @Override
    public void onDisconnect(Runnable disconnectListener) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Nonnull
    @Override
    public Flux<MessageConnection> onConnection() {
        return Flux.just(this);
    }

    @Nonnull
    @Override
    public Mono<Void> publish(@Nonnull TopicMessage message) {
        return Mono
            .justOrEmpty(DeviceMessageUtils.convert(message))
            .flatMap(this::doIndex);
    }

    private Mono<Void> doIndex(DeviceMessage message) {
        Map<String, Object> headers = Optional.ofNullable(message.getHeaders()).orElse(Collections.emptyMap());

        String productId = (String) headers.get("productId");

        DeviceOperationLogEntity operationLog = new DeviceOperationLogEntity();
        operationLog.setId(IDGenerator.MD5.generate());
        operationLog.setDeviceId(message.getDeviceId());
        operationLog.setCreateTime(new Date(message.getTimestamp()));
        operationLog.setProductId(productId);
        operationLog.setType(DeviceLogType.of(message));

        Mono<Void> thenJob = null;
        if (message instanceof EventMessage) {
            operationLog.setContent(JSON.toJSONString(((EventMessage) message).getData()));
            //上报属性
            thenJob = doIndexEventMessage(headers, ((EventMessage) message));
        } else if (message instanceof DeviceOfflineMessage) {
            operationLog.setContent("设备离线");
        } else if (message instanceof DeviceOnlineMessage) {
            operationLog.setContent("设备上线");
        } else if (message instanceof ReportPropertyMessage) {
            ReportPropertyMessage reply = (ReportPropertyMessage) message;
            Map<String, Object> properties = reply.getProperties();
            if (MapUtils.isNotEmpty(properties)) {
                operationLog.setContent(properties);
                thenJob = doIndexPropertiesMessage(headers, message, properties);
            }
        } else if (message instanceof ReadPropertyMessageReply) {
            ReadPropertyMessageReply reply = (ReadPropertyMessageReply) message;
            if (reply.isSuccess()) {
                Map<String, Object> properties = reply.getProperties();
                operationLog.setContent(properties);
                thenJob = doIndexPropertiesMessage(headers, message, properties);
            } else {
                log.warn("读取设备:{} 属性失败", reply.getDeviceId());
            }
        } else if (message instanceof WritePropertyMessageReply) {
            WritePropertyMessageReply reply = (WritePropertyMessageReply) message;
            if (reply.isSuccess()) {
                Map<String, Object> properties = reply.getProperties();
                operationLog.setContent(properties);
                thenJob = doIndexPropertiesMessage(headers, message, properties);
            } else {
                log.warn("修改设备:{} 属性失败", reply.getDeviceId());
            }
        } else if (message instanceof FunctionInvokeMessageReply) {
            operationLog.setContent(JSON.toJSONString(((FunctionInvokeMessageReply) message).getOutput()));
        } else {
            operationLog.setContent(JSON.toJSONString(message));
        }
        if (thenJob == null) {
            thenJob = Mono.empty();
        }
        return timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceLogMetric(productId))
            .save(TimeSeriesData.of(message.getTimestamp(), operationLog.toSimpleMap()))
            .then(thenJob);

    }

    protected Mono<Void> doIndexPropertiesMessage(Map<String, Object> headers,
                                                  DeviceMessage message,
                                                  Map<String, Object> properties) {
        String productId = (String) headers.get("productId");

        return registry
            .getDevice(message.getDeviceId())
            .flatMap(device -> device.getMetadata()
                .flatMap(metadata -> {
                    Map<String, PropertyMetadata> propertyMetadata = metadata.getProperties().stream()
                        .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity()));
                    return Flux.fromIterable(properties.entrySet())
                        .map(entry -> {

                            DevicePropertiesEntity entity = DevicePropertiesEntity.builder()
                                .deviceId(device.getDeviceId())
                                .timestamp(message.getTimestamp())
                                .property(entry.getKey())
                                .propertyName(entry.getKey())
                                .orgId((String) headers.get("orgId"))
                                .productId(productId)
                                .build()
                                .withValue(propertyMetadata.get(entry.getKey()), entry.getValue());

                            return TimeSeriesData.of(message.getTimestamp(), entity.toMap());
                        })
                        .flatMap(data -> timeSeriesManager.getService(DeviceTimeSeriesMetric.devicePropertyMetric(productId)).save(data))
                        .then();
                }));
    }

    protected Mono<Void> doIndexEventMessage(Map<String, Object> headers, EventMessage message) {
        String productId = (String) headers.get("productId");

        return registry.getDevice(message.getDeviceId())
            .flatMap(device -> device.getMetadata()
                .map(metadata -> {
                    Object value = message.getData();
                    DataType dataType = metadata
                        .getEvent(message.getEvent())
                        .map(EventMetadata::getType)
                        .orElseGet(UnknownType::new);
                    Map<String, Object> data = new HashMap<>(headers);
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
            .flatMap(data -> timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceEventMetric(productId, message.getEvent())).save(data));
    }

    /**
     * @return 订阅信息
     * @see org.jetlinks.community.device.message.DeviceMessageConnector
     */
    @Nonnull
    @Override
    public Flux<Subscription> onSubscribe() {
        //订阅设备相关所有消息
        return Flux.just(new Subscription("/device/**"));
    }

    @Nonnull
    @Override
    public Flux<Subscription> onUnSubscribe() {
        return Flux.empty();
    }

    @Override
    public boolean isShareCluster() {
        return false;
    }
}
