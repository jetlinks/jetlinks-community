package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.timeseries.TimeSeriesService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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


    Flux<MeasurementValue> fromHistory(String deviceId, int history, boolean format) {
        return history <= 0 ? Flux.empty() : QueryParamEntity.newQuery()
            .doPaging(0, history)
            .where("deviceId", deviceId)
            .and("property",metadata.getId())
            .execute(timeSeriesService::query)
            .map(data -> SimpleMeasurementValue.of(format ?
                data.get("formatValue").orElse("/") :
                data.get("value").orElse(null), data.getTimestamp()));
    }

    Flux<MeasurementValue> fromRealTime(String deviceId, boolean format) {
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
            .map(msg -> SimpleMeasurementValue.of(format ? metadata.getValueType().format(msg.get(metadata.getId())) : msg.get(metadata.getId()), System.currentTimeMillis()));
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
            return metadata.getValueType();
        }

        @Override
        public ConfigMetadata getParams() {
            // TODO: 2020/1/15
            return null;
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            boolean format = parameter.getBoolean("format").orElse(true);

            return Mono.justOrEmpty(parameter.getString("deviceId"))
                .flatMapMany(deviceId -> {
                    int history = parameter.getInt("history").orElse(0);
                    //合并历史数据和实时数据
                    return Flux.concat(
                        //查询历史数据
                        fromHistory(deviceId, history, format)
                        ,
                        //从消息网关订阅实时事件消息
                        fromRealTime(deviceId, format)
                    );
                });
        }
    }
}
