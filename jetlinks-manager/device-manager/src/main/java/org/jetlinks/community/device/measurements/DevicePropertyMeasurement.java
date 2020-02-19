package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DevicePropertyMeasurement extends StaticMeasurement {

    private PropertyMetadata metadata;

    private MessageGateway messageGateway;

    private TimeSeriesService timeSeriesService;

    public DevicePropertyMeasurement(MessageGateway messageGateway, PropertyMetadata metadata, TimeSeriesService timeSeriesService) {
        super(MetadataMeasurementDefinition.of(metadata));
        this.messageGateway = messageGateway;
        this.metadata = metadata;
        this.timeSeriesService = timeSeriesService;
        addDimension(new RealTimeDevicePropertyDimension());
    }


    Map<String, Object> createValue(Object value) {
        Map<String, Object> values = new HashMap<>();
        DataType type = metadata.getValueType();
        value = type instanceof Converter ? ((Converter<?>) type).convert(value) : value;
        values.put("value", value);
        values.put("formatValue", type.format(value));
        return values;
    }

    Flux<MeasurementValue> fromHistory(String deviceId, int history) {
        return history <= 0 ? Flux.empty() : QueryParamEntity.newQuery()
            .doPaging(0, history)
            .where("deviceId", deviceId)
            .and("property", metadata.getId())
            .execute(timeSeriesService::query)
            .map(data -> SimpleMeasurementValue.of(createValue(data.get("value").orElse(null)), data.getTimestamp()));
    }

    Flux<MeasurementValue> fromRealTime(String deviceId) {
        return messageGateway
            .subscribe(Stream.of(
                "/device/" + deviceId + "/message/property/report"
                , "/device/" + deviceId + "/message/property/*/reply")
                .map(Subscription::new)
                .collect(Collectors.toList()), true)
            .flatMap(val -> Mono.justOrEmpty(DeviceMessageUtils.convert(val)))
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
        .add("history", "历史数据量", "查询出历史数据后开始推送实时数据", new IntType().min(0).expand("defaultValue", 10));

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
            return metadata.getValueType();
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
