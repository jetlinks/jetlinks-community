package org.jetlinks.community.device.message.writer;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.community.device.enums.DeviceLogType;
import org.jetlinks.community.device.events.handler.DeviceEventIndex;
import org.jetlinks.community.device.events.handler.DeviceIndexProvider;
import org.jetlinks.community.device.events.handler.ValueTypeTranslator;
import org.jetlinks.community.device.logger.DeviceOperationLog;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.gateway.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * 用于将设备消息写入到ElasticSearch的消息连接器
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class ElasticDeviceMessageWriterConnector
    implements MessageConnector,
    MessageConnection,
    MessageSubscriber {

//    public String[] deviceConfigs = {"orgId", "productId"};

    public ElasticSearchService searchService;

    public DeviceRegistry registry;

    public ElasticDeviceMessageWriterConnector(ElasticSearchService searchService, DeviceRegistry registry) {
        this.searchService = searchService;
        this.registry = registry;
    }

    @Nonnull
    @Override
    public String getId() {
        return "device-message-elastic-writer";
    }

    @Override
    public String getName() {
        return "写入设备消息到ElasticSearch";
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
    public Mono<Void> publish(@Nonnull TopicMessage message) {
        return Mono
            .justOrEmpty(DeviceMessageUtils.convert(message))
            .flatMap(this::doIndex);
    }

    private Mono<Void> doIndex(DeviceMessage message) {
        Map<String, Object> headers = Optional.ofNullable(message.getHeaders()).orElse(Collections.emptyMap());

        String productId = (String) headers.get("productId");

        DeviceOperationLog operationLog = new DeviceOperationLog();
        operationLog.setDeviceId(message.getDeviceId());
        operationLog.setCreateTime(new Date(message.getTimestamp()));
        operationLog.setProductId(productId);
        operationLog.setType(DeviceLogType.of(message));

        Mono<Void> thenJob = null;
        if (message instanceof EventMessage) {
            operationLog.setContent(JSON.toJSONString(((EventMessage) message).getData()));
            thenJob = doIndexEventMessage(headers, ((EventMessage) message));
            if (message.getHeader(Headers.reportProperties).isPresent()) {
                Object response = ((EventMessage) message).getData();
                if (response instanceof Map) {
                    thenJob = thenJob.then(doIndexPropertiesMessage(headers, message, ((Map) response)));
                }
            }
        } else if (message instanceof DeviceOfflineMessage) {
            operationLog.setContent("设备离线");
        } else if (message instanceof DeviceOnlineMessage) {
            operationLog.setContent("设备上线");
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
        return searchService
            .commit(DeviceIndexProvider.DEVICE_OPERATION, operationLog.toSimpleMap())
            .then(thenJob);

    }

    protected Mono<Void> doIndexPropertiesMessage(Map<String, Object> headers,
                                                  DeviceMessage message,
                                                  Map<String, Object> properties) {
        return registry
            .getDevice(message.getDeviceId())
            .flatMap(device -> device.getMetadata()
                .flatMap(metadata -> {
                    Map<String, PropertyMetadata> propertyMetadata = metadata.getProperties().stream()
                        .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity()));
                    return Flux.fromIterable(properties.entrySet())
                        .map(entry -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", device.getDeviceId());
                            data.put("timestamp", message.getTimestamp());
                            data.put("property", entry.getKey());
                            data.put("propertyName", entry.getKey());
                            data.put("orgId", headers.get("orgId"));
                            data.put("productId", headers.get("productId"));
                            data.put("value", entry.getValue().toString());
                            ofNullable(propertyMetadata.get(entry.getKey()))
                                .ifPresent(prop -> {
                                    DataType type = prop.getValueType();
                                    data.put("propertyName", prop.getName());
                                    if (type instanceof NumberType) {
                                        NumberType<?> numberType = (NumberType<?>) type;
                                        data.put("numberValue", new BigDecimal(numberType.convert(entry.getValue()).toString()));
                                    } else if (type instanceof DateTimeType) {
                                        DateTimeType dateTimeType = (DateTimeType) type;
                                        data.put("timeValue", dateTimeType.convert(entry.getValue()));
                                    } else if (type instanceof ObjectType) {
                                        ObjectType ObjectType = (ObjectType) type;
                                        data.put("objectValue", ObjectType.convert(entry.getValue()));
                                    } else {
                                        data.put("stringValue", String.valueOf(entry.getValue()));
                                    }
                                    ofNullable(type.format(entry.getValue()))
                                        .map(String::valueOf)
                                        .ifPresent(val -> data.put("formatValue", val));
                                });
                            return data;
                        }).flatMap(data ->
                            searchService.commit(
                                DeviceEventIndex.getDevicePropertiesIndex((String) headers.get("productId")),
                                data
                            ))
                        .then();
                }));
    }

    protected Mono<Void> doIndexEventMessage(Map<String, Object> headers, EventMessage message) {
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
                    return data;
                })).flatMap(data -> searchService.commit(DeviceEventIndex.getDeviceEventIndex((String) headers.get("productId"), message.getEvent()), data));
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
    public Flux<Subscription> onUnSubscribe() {
        return Flux.empty();
    }

    @Override
    public boolean isShareCluster() {
        return false;
    }
}
