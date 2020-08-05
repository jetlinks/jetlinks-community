package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

class DevicePropertiesMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final TimeSeriesService timeSeriesService;

    private final DeviceMetadata metadata;

    private final String productId;

    public DevicePropertiesMeasurement(String productId,
                                       EventBus eventBus,
                                       DeviceMetadata deviceMetadata,
                                       TimeSeriesService timeSeriesService) {
        super(MeasurementDefinition.of("properties", "属性记录"));
        this.productId = productId;
        this.eventBus = eventBus;
        this.timeSeriesService = timeSeriesService;
        this.metadata = deviceMetadata;
        addDimension(new RealTimeDevicePropertyDimension());
        addDimension(new HistoryDevicePropertyDimension());

    }

    Flux<SimpleMeasurementValue> fromHistory(String deviceId, int history) {
        return history <= 0 ? Flux.empty() : Flux.fromIterable(metadata.getProperties())
            .flatMap(propertyMetadata -> QueryParamEntity.newQuery()
                .doPaging(0, history)
                .where("deviceId", deviceId)
                .and("property", propertyMetadata.getId())
                .execute(timeSeriesService::query)
                .map(data -> SimpleMeasurementValue.of(createValue(propertyMetadata.getId(), data.get("value").orElse(null)), data.getTimestamp()))
                .sort(MeasurementValue.sort()));
    }

    Map<String, Object> createValue(String property, Object value) {
        return metadata.getProperty(property)
            .map(meta -> {
                Map<String, Object> values = new HashMap<>();
                DataType type = meta.getValueType();
                Object val = type instanceof Converter ? ((Converter<?>) type).convert(value) : value;
                values.put("formatValue", type.format(val));
                values.put("value", val);
                values.put("property", property);

                return values;
            })
            .orElseGet(() -> {
                Map<String, Object> values = new HashMap<>();
                values.put("formatValue", value);
                values.put("value", value);
                values.put("property", property);
                return values;
            });
    }

    Flux<MeasurementValue> fromRealTime(String deviceId) {

        org.jetlinks.core.event.Subscription subscription= org.jetlinks.core.event.Subscription.of(
            "realtime-device-properties-measurement",
            new String[]{
                "/device/" + productId + "/" + deviceId + "/message/property/report",
                "/device/" + productId + "/" + deviceId + "/message/property/*/reply"
            },
            org.jetlinks.core.event.Subscription.Feature.local, Subscription.Feature.broker
        );

        return
            eventBus
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
                .flatMap(map -> Flux.fromIterable(map.entrySet()))
                .map(kv -> SimpleMeasurementValue.of(createValue(kv.getKey(), kv.getValue()), System.currentTimeMillis()))
            ;
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"));

    /**
     * 历史设备事件
     */
    private class HistoryDevicePropertyDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.history;
        }

        @Override
        public DataType getValueType() {
            return new ObjectType()
                .addProperty("property","属性", StringType.GLOBAL)
                .addProperty("value","值", StringType.GLOBAL)
                .addProperty("formatValue","格式化值", StringType.GLOBAL);
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
                    //合并历史数据和实时数据
                    return fromHistory(deviceId, history);
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
                .addProperty("property","属性", StringType.GLOBAL)
                .addProperty("value","值", StringType.GLOBAL)
                .addProperty("formatValue","格式化值", StringType.GLOBAL);
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
