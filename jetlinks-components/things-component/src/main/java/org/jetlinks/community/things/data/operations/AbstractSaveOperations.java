package org.jetlinks.community.things.data.operations;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.message.DeviceLogMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.event.ThingEventMessage;
import org.jetlinks.core.message.property.PropertyMessage;
import org.jetlinks.core.message.property.ThingReportPropertyMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingTemplate;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.core.utils.TimestampUtils;
import org.jetlinks.community.things.ThingConstants;
import org.jetlinks.community.things.data.ThingLogType;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;

import static org.jetlinks.community.things.data.ThingsDataConstants.*;

@Slf4j
@AllArgsConstructor
public abstract class AbstractSaveOperations implements SaveOperations {

    protected final ThingsRegistry registry;

    protected final MetricBuilder metricBuilder;

    protected final DataSettings settings;

    @Override
    public final Mono<Void> save(ThingMessage thingMessage) {
        return this
            .convertMessageToTimeSeriesData(thingMessage)
            .flatMap(tp2 -> this.doSave(tp2.getT1(), tp2.getT2()))
            .then();
    }

    @Override
    public final Mono<Void> save(Collection<? extends ThingMessage> thingMessage) {
        return save(Flux.fromIterable(thingMessage));
    }

    @Override
    public final Mono<Void> save(Publisher<? extends ThingMessage> thingMessage) {
        return Flux
            .from(thingMessage)
            .flatMap(this::convertMessageToTimeSeriesData)
            .groupBy(Tuple2::getT1)
            .flatMap(group -> this.doSave(group.key(), group.map(Tuple2::getT2)))
            .then();
    }

    protected Map<String, Object> createLogData(ThingMessage message) {
        Map<String, Object> data = Maps.newHashMapWithExpectedSize(8);
        data.put(COLUMN_ID, IDGenerator.SNOW_FLAKE_STRING.generate());
        data.put(metricBuilder.getThingIdProperty(), message.getThingId());
        data.put(COLUMN_TIMESTAMP, message.getTimestamp());
        data.put(COLUMN_CREATE_TIME, System.currentTimeMillis());
        data.put(COLUMN_MESSAGE_ID, message.getMessageId());
        data.put(COLUMN_LOG_TYPE, ThingLogType.of(message).name());
        String log;
        if (message instanceof DeviceLogMessage) {
            log = ((DeviceLogMessage) message).getLog();
        } else {
            log = ObjectMappers.toJsonString(message.toJson());
        }
        data.put(COLUMN_LOG_CONTENT, log);
        return data;
    }

    protected String getTemplateIdFromMessage(ThingMessage message) {
        String templateId = message.getHeader(Headers.productId).orElse(null);
        if (templateId == null) {
            templateId = message.getHeader(ThingConstants.templateId).orElse(null);
        }
        return templateId == null ? "null" : templateId;
    }

    protected Flux<Tuple2<String, TimeSeriesData>> convertMessageToTimeSeriesData(ThingMessage message) {
        boolean ignoreStorage = message.getHeaderOrDefault(Headers.ignoreStorage);
        boolean ignoreLog = message.getHeaderOrDefault(Headers.ignoreLog);
        if (ignoreStorage && ignoreLog) {
            return Flux.empty();
        }
        String templateId = getTemplateIdFromMessage(message);
        List<Publisher<Tuple2<String, TimeSeriesData>>> all = new ArrayList<>(2);
        //没有忽略数据存储
        if (!ignoreStorage) {
            //事件上报
            if (message instanceof ThingEventMessage) {
                all.add(convertEventMessageToTimeSeriesData(templateId, ((ThingEventMessage) message)));
            }
            //属性相关消息
            else if (message instanceof PropertyMessage) {
                //配置了只保存属性上报
                if (!settings.getProperty().isOnlySaveReport()
                    || (message instanceof ThingReportPropertyMessage)) {
                    PropertyMessage propertyMessage = ((PropertyMessage) message);
                    Map<String, Object> properties = propertyMessage.getProperties();
                    if (MapUtils.isNotEmpty(properties)) {
                        //属性源时间
                        Map<String, Long> propertiesTimes = propertyMessage.getPropertySourceTimes();
                        if (propertiesTimes == null) {
                            propertiesTimes = Collections.emptyMap();
                        }
                        all.add(convertProperties(templateId, message, properties, propertiesTimes));
                    }
                }

            }
        }
        //配置了记录日志,并且消息头里没有标记忽略日志
        if (settings.getLogFilter().match(message.getMessageType()) && !ignoreLog) {
            all.add(createDeviceMessageLog(templateId, message));
        }

        return Flux.merge(all);
    }

    private Mono<Tuple2<String, TimeSeriesData>> convertEventMessageToTimeSeriesData(String templateId, ThingEventMessage message) {

        return registry
            .getTemplate(message.getThingType(), templateId)
            .flatMap(thing -> {
                //配置了所有事件存储在同一个表中时,这时支持设备自定义事件物模型,直接获取设备的物模型
                if (settings.getEvent().eventIsAllInOne()) {
                    return thing.getMetadata();
                }
                //获取设备产品的物模型,为什么不直接获取模版?因为后期可能支持多版本.
                return registry
                    .getThing(message.getThingType(), message.getThingId())
                    .flatMap(Thing::getTemplate)
                    .flatMap(ThingTemplate::getMetadata);
            })
            .<TimeSeriesData>handle((metadata, sink) -> {
                if (settings.getEvent().shouldIgnoreUndefined()
                    && metadata.getEventOrNull(message.getEvent()) == null) {
                    log.warn("{}[{}] event [{}] metadata undefined", message.getThingType(), message.getThingId(), message.getEvent());
                    return;
                }
                Map<String, Object> data = createEventData(message, metadata);
                sink.next(TimeSeriesData.of(TimestampUtils.toMillis(message.getTimestamp()), data));
            })
            .map(data -> Tuples.of(createEventMetric(message.getThingType(), templateId, message.getThingId(), message.getEvent()), data));
    }

    private String createEventMetric(String thingType,
                                     String thingTemplateId,
                                     String thingId,
                                     String eventId) {
        return settings.getEvent().eventIsAllInOne()
            ? metricBuilder.createEventAllInOneMetric(thingType, thingTemplateId, thingId)
            : metricBuilder.createEventMetric(thingType, thingTemplateId, thingId, eventId);
    }

    protected Object convertValue(Object value, DataType type) {
        if (type instanceof Converter) {
            return ((Converter<?>) type).convert(value);
        }
        return value;
    }

    protected Map<String, Object> createEventData(ThingEventMessage message, ThingMetadata metadata) {
        Object value = message.getData();
        DataType dataType = metadata
            .getEvent(message.getEvent())
            .map(EventMetadata::getType)
            .orElseGet(UnknownType::new);
        Object tempValue = convertValue(value, dataType);
        Map<String, Object> data;
        //使用json字符存储数据
        if (settings.getEvent().isUsingJsonString()) {
            data = Maps.newHashMapWithExpectedSize(16);
            data.put(COLUMN_EVENT_VALUE, tempValue instanceof String ? tempValue : ObjectMappers.toJsonString(tempValue));
        } else {
            if (tempValue instanceof Map) {
                @SuppressWarnings("all")
                Map<String, Object> mapValue = ((Map) tempValue);
                int size = mapValue.size();
                data = Maps.newHashMapWithExpectedSize(size);
                data.putAll(mapValue);
                //严格模式,只记录物模型中记录的字段
                if (settings.isStrict()) {
                    if (dataType instanceof ObjectType) {
                        Set<String> nonexistent = new HashSet<>(data.keySet());
                        ObjectType objType = ((ObjectType) dataType);
                        for (PropertyMetadata property : objType.getProperties()) {
                            nonexistent.remove(property.getId());
                        }
                        nonexistent.forEach(data::remove);
                    }
                }
            } else {
                data = Maps.newHashMapWithExpectedSize(16);
                data.put(COLUMN_EVENT_VALUE, tempValue);
            }
        }
        //所有数据都存储在一个表里时,给表添加一个event值
        if (settings.getEvent().eventIsAllInOne()) {
            data.put(COLUMN_EVENT_ID, message.getEvent());
        }
        data.put(COLUMN_ID, createEventDataId(message));
        data.put(metricBuilder.getThingIdProperty(), message.getThingId());
        data.put(COLUMN_CREATE_TIME, System.currentTimeMillis());
        data.put(COLUMN_TIMESTAMP, message.getTimestamp());

        return data;
    }

    protected String createEventDataId(ThingMessage message) {
        return DigestUtils
            .md5Hex(StringBuilderUtils.buildString(message, (msg, builder) -> builder
                .append(msg.getThingId())
                .append('-')
                .append(msg.getTimestamp())));
    }

    private Mono<Tuple2<String, TimeSeriesData>> createDeviceMessageLog(String templateId,
                                                                        ThingMessage message) {


        return Mono.just(Tuples.of(
            metricBuilder.createLogMetric(message.getThingType(), templateId, message.getThingId()),
            TimeSeriesData.of(message.getTimestamp(), createLogData(message))));
    }

    protected abstract Flux<Tuple2<String, TimeSeriesData>> convertProperties(String templateId,
                                                                              ThingMessage message,
                                                                              Map<String, Object> properties,
                                                                              Map<String, Long> propertySourceTimes);

    protected abstract Mono<Void> doSave(String metric, TimeSeriesData data);

    protected abstract Mono<Void> doSave(String metric, Flux<TimeSeriesData> data);

    @Override
    public Flux<Feature> getFeatures() {
        if (settings.getEvent().eventIsAllInOne()) {
            return Flux.empty();
        } else {
            //事件不支持新增以及修改
            return Flux.just(MetadataFeature.eventNotInsertable,
                             MetadataFeature.eventNotModifiable
            );
        }
    }
}
