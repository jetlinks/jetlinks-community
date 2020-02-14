package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.timeseries.TimeSeriesService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

class DeviceEventMeasurement extends StaticMeasurement {

    public EventMetadata eventMetadata;

    public MessageGateway messageGateway;

    private TimeSeriesService eventTsService;

    public DeviceEventMeasurement(MessageGateway messageGateway, EventMetadata eventMetadata, TimeSeriesService eventTsService) {
        super(MetadataMeasurementDefinition.of(eventMetadata));
        this.messageGateway = messageGateway;
        this.eventMetadata = eventMetadata;
        this.eventTsService = eventTsService;
        addDimension(new RealTimeDeviceEventDimension());
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"))
        .add("history", "历史数据量", "查询出历史数据后开始推送实时数据", new IntType().min(0).expand("defaultValue", 10));


    Flux<MeasurementValue> fromHistory(String deviceId, int history) {
        return history <= 0 ? Flux.empty() : QueryParamEntity.newQuery()
            .doPaging(0, history)
            .where("deviceId", deviceId)
            .execute(eventTsService::query)
            .map(data -> SimpleMeasurementValue.of(data.getData(), data.getTimestamp()));
    }


    Flux<MeasurementValue> fromRealTime(String deviceId) {
        return messageGateway
            .subscribe(Collections.singletonList(new Subscription("/device/" + deviceId + "/message/event/" + eventMetadata.getId())), true)
            .flatMap(val -> Mono.justOrEmpty(DeviceMessageUtils.convert(val)))
            .cast(EventMessage.class)
            .map(msg -> SimpleMeasurementValue.of(msg.getData(), msg.getTimestamp()));
    }

    /**
     * 实时设备事件
     */
    class RealTimeDeviceEventDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return eventMetadata.getType();
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
