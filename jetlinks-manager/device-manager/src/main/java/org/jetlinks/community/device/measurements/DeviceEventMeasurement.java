package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.service.data.DeviceDataService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class DeviceEventMeasurement extends StaticMeasurement {

    public EventMetadata eventMetadata;

    public EventBus eventBus;

    private final DeviceDataService deviceDataService;

    private final String productId;

    public DeviceEventMeasurement(String productId,
                                  EventBus eventBus,
                                  EventMetadata eventMetadata,
                                  DeviceDataService deviceDataService) {
        super(MetadataMeasurementDefinition.of(eventMetadata));
        this.productId = productId;
        this.eventBus = eventBus;
        this.eventMetadata = eventMetadata;
        this.deviceDataService = deviceDataService;
        addDimension(new RealTimeDeviceEventDimension());
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"))
        .add("history", "历史数据量", "查询出历史数据后开始推送实时数据", new IntType().min(0).expand("defaultValue", 10));


    Flux<SimpleMeasurementValue> fromHistory(String deviceId, int history) {
        return history <= 0 ? Flux.empty() : QueryParamEntity.newQuery()
            .doPaging(0, history)
            .where("deviceId", deviceId)
            .execute(q->deviceDataService.queryEvent(deviceId,eventMetadata.getId(),q,false))
            .map(data -> SimpleMeasurementValue.of(data, data.getTimestamp()))
            .sort(MeasurementValue.sort());
    }


    Flux<MeasurementValue> fromRealTime(String deviceId) {
        Subscription subscription = Subscription
            .of("deviceEventMeasurement", "/device/" + productId + "/" + deviceId + "/message/event/" + eventMetadata.getId(), Subscription.Feature.local);

        return eventBus
            .subscribe(subscription, DeviceMessage.class)
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
