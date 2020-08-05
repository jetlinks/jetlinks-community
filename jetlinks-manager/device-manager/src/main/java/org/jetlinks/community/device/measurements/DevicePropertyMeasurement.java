package org.jetlinks.community.device.measurements;

import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.Interval;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class DevicePropertyMeasurement extends StaticMeasurement {

    private final PropertyMetadata metadata;

    private final EventBus eventBus;

    private final TimeSeriesService timeSeriesService;

    private final String productId;

    public DevicePropertyMeasurement(String productId,
                                     EventBus eventBus,
                                     PropertyMetadata metadata,
                                     TimeSeriesService timeSeriesService) {
        super(MetadataMeasurementDefinition.of(metadata));
        this.productId = productId;
        this.eventBus = eventBus;
        this.metadata = metadata;
        this.timeSeriesService = timeSeriesService;
        addDimension(new RealTimeDevicePropertyDimension());
        addDimension(new HistoryDevicePropertyDimension());
        if (metadata.getValueType() instanceof NumberType) {
            addDimension(new AggDevicePropertyDimension());
        }
    }


    Map<String, Object> createValue(Object value) {
        Map<String, Object> values = new HashMap<>();
        DataType type = metadata.getValueType();
        value = type instanceof Converter ? ((Converter<?>) type).convert(value) : value;
        values.put("value", value);
        values.put("formatValue", type.format(value));
        return values;
    }

    Flux<SimpleMeasurementValue> fromHistory(String deviceId, int history) {
        return history <= 0 ? Flux.empty() : QueryParamEntity.newQuery()
            .doPaging(0, history)
            .where("deviceId", deviceId)
            .and("property", metadata.getId())
            .execute(timeSeriesService::query)
            .map(data -> SimpleMeasurementValue.of(createValue(data.get("value").orElse(null)), data.getTimestamp()))
            .sort(MeasurementValue.sort());
    }

    Flux<MeasurementValue> fromRealTime(String deviceId) {
        org.jetlinks.core.event.Subscription subscription = org.jetlinks.core.event.Subscription.of(
            "realtime-device-property-measurement",
            new String[]{
                "/device/" + productId + "/" + deviceId + "/message/property/report",
                "/device/" + productId + "/" + deviceId + "/message/property/*/reply"
            },
            org.jetlinks.core.event.Subscription.Feature.local, org.jetlinks.core.event.Subscription.Feature.broker
        );

        return eventBus
            .subscribe(subscription, DeviceMessage.class)
            .flatMap(msg -> {
                if (msg instanceof ReportPropertyMessage) {
                    return Mono.justOrEmpty(((ReportPropertyMessage) msg).getProperties());
                }
                if (msg instanceof ReadPropertyMessageReply) {
                    return Mono.justOrEmpty(((ReadPropertyMessageReply) msg).getProperties());
                }
                if (msg instanceof WritePropertyMessageReply) {
                    return Mono.justOrEmpty(((WritePropertyMessageReply) msg).getProperties());
                }
                return Mono.empty();
            })
            .filter(msg -> msg.containsKey(metadata.getId()))
            .map(msg -> SimpleMeasurementValue.of(createValue(msg.get(metadata.getId())), System.currentTimeMillis()));
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"))
        .add("history", "历史数据量", "查询出历史数据后开始推送实时数据", new IntType().min(0).expand("defaultValue", 10))
        .add("from", "时间从", "", StringType.GLOBAL)
        .add("to", "时间至", "", StringType.GLOBAL);
    ;

    static ConfigMetadata aggConfigMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备ID", "", StringType.GLOBAL)
        .add("time", "周期", "例如: 1h,10m,30s", StringType.GLOBAL)
        .add("agg", "聚合类型", "count,sum,avg,max,min", StringType.GLOBAL)
        .add("format", "时间格式", "如: MM-dd:HH", StringType.GLOBAL)
        .add("limit", "最大数据量", "", StringType.GLOBAL)
        .add("from", "时间从", "", StringType.GLOBAL)
        .add("to", "时间至", "", StringType.GLOBAL);

    /**
     * 聚合数据
     */
    private class AggDevicePropertyDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        public DataType getValueType() {
            return new ObjectType()
                .addProperty("value", "数据", new ObjectType()
                    .addProperty("property", StringType.GLOBAL)
                    .addProperty("value", metadata.getValueType())
                    .addProperty("formatValue", StringType.GLOBAL))
                .addProperty("timeString", "时间", StringType.GLOBAL);
        }

        @Override
        public ConfigMetadata getParams() {
            return aggConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {

            return AggregationQueryParam.of()
                .agg("numberValue", "value", parameter.getString("agg").map(String::toUpperCase).map(Aggregation::valueOf).orElse(Aggregation.AVG))
                .filter(query -> query
                    .where("property", metadata.getId())
                    .and("deviceId", parameter.getString("deviceId").orElse(null))
                )
                .limit(parameter.getInt("limit", 10))
                .groupBy(parameter.getInterval("time", Interval.ofSeconds(10)), parameter.getString("format", "HH:mm:ss"))
                .from(parameter.getDate("from").orElseGet(() -> Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())))
                .to(parameter.getDate("to").orElse(new Date()))
                .execute(timeSeriesService::aggregation)
                .index((index, data) -> SimpleMeasurementValue.of(
                    createValue(data.getInt("value").orElse(0)),
                    data.getString("time").orElse(""),
                    index))
                .sort();

        }
    }

    /**
     * 历史设备数据
     */
    private class HistoryDevicePropertyDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.history;
        }

        @Override
        public DataType getValueType() {
            return new ObjectType()
                .addProperty("property", "属性", StringType.GLOBAL)
                .addProperty("value", "值", metadata.getValueType())
                .addProperty("formatValue", "格式化值", StringType.GLOBAL);
        }

        @Override
        public ConfigMetadata getParams() {
            return configMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            return Mono.justOrEmpty(parameter.getString("deviceId"))
                .flatMapMany(deviceId -> {
                    int history = parameter.getInt("history").orElse(1);

                    return QueryParamEntity.newQuery()
                        .doPaging(0, history)
                        .where("deviceId", deviceId)
                        .and("property", metadata.getId())
                        .as(query -> query
                            .gte("timestamp", parameter.getDate("from").orElse(null))
                            .lte("timestamp", parameter.getDate("to").orElse(null)))
                        .execute(timeSeriesService::query)
                        .map(data -> SimpleMeasurementValue.of(
                            createValue(data.get("value").orElse(null)),
                            DateFormatter.toString(new Date(data.getTimestamp()), parameter.getString("timeFormat","HH:mm:ss")),
                            data.getTimestamp()))
                        .sort(MeasurementValue.sort());
                });
        }
    }

    /**
     * 实时设备事件
     */
    private class RealTimeDevicePropertyDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return new ObjectType()
                .addProperty("property", "属性", StringType.GLOBAL)
                .addProperty("value", "值", metadata.getValueType())
                .addProperty("formatValue", "格式化值", StringType.GLOBAL);
        }

        @Override
        public ConfigMetadata getParams() {
            return configMetadata;
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            return Mono.justOrEmpty(parameter.getString("deviceId"))
                .flatMapMany(deviceId -> {
                    int history = parameter.getInt("history").orElse(0);
                    //合并历史数据和实时数据
                    return Flux.concat(
                        //查询历史数据
                        fromHistory(deviceId, history)
                        ,
                        //从消息网关订阅实时事件消息
                        fromRealTime(deviceId)
                    );
                });
        }
    }


}
