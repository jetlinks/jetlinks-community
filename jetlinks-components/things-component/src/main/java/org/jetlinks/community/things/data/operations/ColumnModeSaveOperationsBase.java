package org.jetlinks.community.things.data.operations;

import com.google.common.collect.Maps;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.metadata.MetadataFeature;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;

import static org.jetlinks.community.things.data.ThingsDataConstants.*;

public abstract class ColumnModeSaveOperationsBase extends AbstractSaveOperations {

    public ColumnModeSaveOperationsBase(ThingsRegistry registry, MetricBuilder metricBuilder, DataSettings settings) {
        super(registry, metricBuilder, settings);
    }

    protected String createPropertyDataId(ThingMessage message) {
        if (!useTimestampId(message)) {
            return randomId();
        }
        return DigestUtils.md5Hex(
            StringBuilderUtils
                .buildString(message, (m, builder) -> {
                    builder
                        .append(m.getThingId())
                        .append('-')
                        .append(m.getTimestamp());
                })
        );
    }

    @Override
    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String templateId,
                                                                     ThingMessage message,
                                                                     Map<String, Object> properties,
                                                                     Map<String, Long> propertySourceTimes) {
        if (MapUtils.isEmpty(properties)) {
            return Flux.empty();
        }
        return this
            .registry
            .getThing(message.getThingType(), message.getThingId())
            .flatMapMany(device -> device
                .getMetadata()
                .mapNotNull(metadata -> {
                    int size = properties.size();
                    String id = createPropertyDataId(message);
                    Map<String, Object> data = Maps.newLinkedHashMapWithExpectedSize(size);
                    //转换属性数据
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        PropertyMetadata propertyMetadata = metadata.getPropertyOrNull(entry.getKey());
                        //没有配置物模型或者忽略了存储
                        if (propertyMetadata == null || propertyIsIgnoreStorage(propertyMetadata)) {
                            continue;
                        }
                        Object value = convertPropertyValue(entry.getValue(), propertyMetadata);
                        if (null != value) {
                            data.put(entry.getKey(), value);
                        }
                    }
                    //没有属性值,可能全部都配置了不存储
                    if (data.isEmpty()) {
                        return null;
                    }
                    long timestamp = convertTimestamp(message.getTimestamp());
                    data.put(metricBuilder.getThingIdProperty(), message.getThingId());
                    data.put(COLUMN_TIMESTAMP, timestamp);
                    data.put(COLUMN_CREATE_TIME, System.currentTimeMillis());
                    data.put(COLUMN_ID, id);
                    return Tuples.of(metricBuilder.createPropertyMetric(message.getThingType(), templateId, message.getThingId()),
                                     TimeSeriesData.of(timestamp, handlePropertiesData(metadata, data)));
                }));
    }

    protected Map<String, Object> handlePropertiesData(ThingMetadata metadata, Map<String, Object> properties) {
        return properties;
    }


    protected Object convertPropertyValue(Object value, PropertyMetadata metadata) {
        if (value == null || metadata == null) {
            return value;
        }
        //使用json字符串来存储
        if (propertyIsJsonStringStorage(metadata)) {
            return value instanceof String ? String.valueOf(value) : ObjectMappers.toJsonString(value);
        }
        //数字类型直接返回
        if (metadata.getValueType() instanceof NumberType) {
            NumberType<?> type = ((NumberType<?>) metadata.getValueType());
            return convertNumberValue(type, type.convertOriginalNumber(value));
        }
        if (metadata.getValueType() instanceof Converter) {
            return ((Converter<?>) metadata.getValueType()).convert(value);
        }
        return value;
    }

    protected boolean propertyIsJsonStringStorage(PropertyMetadata metadata) {
        return ThingsDataConstants.propertyIsJsonStringStorage(metadata);
    }

    protected Object convertNumberValue(NumberType<?> type, Number value) {
        return value;
    }


    @Override
    protected abstract Mono<Void> doSave(String metric, TimeSeriesData data);

    @Override
    protected abstract Mono<Void> doSave(String metric, Flux<TimeSeriesData> data);

    @Override
    public Flux<Feature> getFeatures() {
        return Flux.concat(
            super.getFeatures(),
            Flux.just(
                MetadataFeature.propertyNotModifiable,
                MetadataFeature.propertyNotInsertable
            )
        );
    }
}
