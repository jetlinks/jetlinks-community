package org.jetlinks.community.things.data.operations;

import com.google.common.collect.Maps;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.core.utils.TimestampUtils;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetlinks.community.things.data.ThingsDataConstants.*;

public abstract class RowModeSaveOperationsBase extends AbstractSaveOperations {

    public RowModeSaveOperationsBase(ThingsRegistry registry, MetricBuilder metricBuilder, DataSettings settings) {
        super(registry, metricBuilder, settings);
    }

    @Override
    protected final Flux<Tuple2<String, TimeSeriesData>> convertProperties(
        String templateId,
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
                .flatMapIterable(metadata -> createPropertyTsData(templateId, metadata, message, properties, propertySourceTimes)));
    }

    private List<Tuple2<String, TimeSeriesData>> createPropertyTsData(String templateId,
                                                                      ThingMetadata metadata,
                                                                      ThingMessage message,
                                                                      Map<String, Object> properties,
                                                                      Map<String, Long> propertySourceTimes) {
        List<Tuple2<String, TimeSeriesData>> data = new ArrayList<>(properties.size());

        String metric = metricBuilder.createPropertyMetric(message.getThingType(), templateId, message.getThingId());

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String property = entry.getKey();
            Object value = entry.getValue();
            //忽略存在没有的属性和忽略存储的属性
            PropertyMetadata propertyMetadata = metadata.getPropertyOrNull(property);
            if (value == null || propertyMetadata == null || propertyIsIgnoreStorage(propertyMetadata)) {
                continue;
            }
            try {
                long timestamp = convertTimestamp(
                    propertySourceTimes.getOrDefault(property, message.getTimestamp()));
                String dataId = createPropertyDataId(property, message, timestamp);

                data.add(
                    Tuples.of(
                        metric,
                        TimeSeriesData.of(timestamp, this
                            .createRowPropertyData(dataId,
                                                   TimestampUtils.toMillis(timestamp),
                                                   message,
                                                   propertyMetadata,
                                                   value))
                    )
                );
            } catch (Throwable err) {
                handlerError("create property[" + property + "] ts data", message, err);
            }

        }
        return data;
    }

    protected boolean propertyIsIgnoreStorage(PropertyMetadata metadata) {
        return ThingsDataConstants.propertyIsIgnoreStorage(metadata);
    }

    protected String createPropertyDataId(String property, ThingMessage message, long timestamp) {
        if (!useTimestampId(message)) {
            return randomId();
        }
        return DigestUtils.md5Hex(
            StringBuilderUtils
                .buildString(property, message, timestamp, (p, m, ts, builder) -> {
                    builder
                        .append(m.getThingId())
                        .append('-')
                        .append(p)
                        .append('-')
                        .append(ts);
                })
        );
    }

    protected Map<String, Object> createRowPropertyData(String id,
                                                        long timestamp,
                                                        ThingMessage message,
                                                        PropertyMetadata property,
                                                        Object value) {
        Map<String, Object> propertyData = Maps.newLinkedHashMapWithExpectedSize(16);
        propertyData.put(COLUMN_ID, id);
        propertyData.put(metricBuilder.getThingIdProperty(), message.getThingId());
        propertyData.put(COLUMN_TIMESTAMP, timestamp);
        propertyData.put(COLUMN_PROPERTY_ID, property.getId());
        propertyData.put(COLUMN_CREATE_TIME, System.currentTimeMillis());

        fillRowPropertyValue(propertyData, property, value);
        return propertyData;
    }

    protected Number convertNumberValue(Number number) {
        //转换数字值
        return number;
    }

    protected void fillRowPropertyValue(Map<String, Object> target, PropertyMetadata property, Object value) {
        if (value == null) {
            return;
        }
        DataType type = property.getValueType();
        //不存储type,没啥意义
        // target.put(COLUMN_PROPERTY_TYPE, type.getId());
        String convertedValue;
        if (type instanceof NumberType) {
            NumberType<?> numberType = (NumberType<?>) type;
            Number number = value instanceof Number
                ? ((Number) value)
                : numberType.convertNumber(value);
            if (number == null) {
                throw new BusinessException.NoStackTrace("error.cannot_convert", 500, value, type.getId());
            }
            convertedValue = String.valueOf(number);
            target.put(COLUMN_PROPERTY_NUMBER_VALUE, convertNumberValue(number));
        } else if (type instanceof DateTimeType) {
            DateTimeType dateTimeType = (DateTimeType) type;
            convertedValue = String.valueOf(value);
            target.put(COLUMN_PROPERTY_TIME_VALUE, dateTimeType.convert(convertedValue));
        } else if (propertyIsJsonStringStorage(property)) {
            //使用json字符来存储
            convertedValue = value instanceof String
                ? String.valueOf(value)
                : ObjectMappers.toJsonString(value);

        } else if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            Object val = objectType.convert(value);
            convertedValue = ObjectMappers.toJsonString(val);
            target.put(COLUMN_PROPERTY_OBJECT_VALUE, val);
        } else if (type instanceof ArrayType) {
            ArrayType objectType = (ArrayType) type;
            Object val = objectType.convert(value);
            convertedValue = ObjectMappers.toJsonString(val);
            target.put(COLUMN_PROPERTY_ARRAY_VALUE, val);
        } else if (type instanceof GeoType) {
            GeoType geoType = (GeoType) type;
            GeoPoint val = geoType.convert(value);
            convertedValue = String.valueOf(val);
            target.put(COLUMN_PROPERTY_GEO_VALUE, val);
        } else {
            convertedValue = String.valueOf(value);
        }
        target.put(COLUMN_PROPERTY_VALUE, convertedValue);
    }

    boolean propertyIsJsonStringStorage(PropertyMetadata metadata) {
        return ThingsDataConstants.propertyIsJsonStringStorage(metadata);
    }


    @Override
    protected abstract Mono<Void> doSave(String metric, TimeSeriesData data);

    @Override
    protected abstract Mono<Void> doSave(String metric, Flux<TimeSeriesData> data);
}
