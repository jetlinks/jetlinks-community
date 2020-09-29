package org.jetlinks.community.device.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.service.data.DeviceDataService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class DeviceEventsMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final DeviceDataService deviceDataService;

    private final DeviceMetadata metadata;

    private final String productId;

    public DeviceEventsMeasurement(String productId,
                                   EventBus eventBus,
                                   DeviceMetadata deviceMetadata,
                                   DeviceDataService deviceDataService) {
        super(MeasurementDefinition.of("events", "事件记录"));
        this.productId = productId;
        this.eventBus = eventBus;
        this.deviceDataService = deviceDataService;
        this.metadata = deviceMetadata;
        addDimension(new RealTimeDevicePropertyDimension());
    }

    static AtomicLong num = new AtomicLong();

    Flux<SimpleMeasurementValue> fromHistory(String deviceId, int history) {
        return history <= 0 ? Flux.empty() : Flux.fromIterable(metadata.getEvents())
            .flatMap(event -> QueryParamEntity.newQuery()
                .doPaging(0, history)
                .execute(q -> deviceDataService.queryEvent(deviceId, event.getId(), q, false))
                .map(data -> SimpleMeasurementValue.of(createValue(event.getId(), data), data.getTimestamp()))
                .sort(MeasurementValue.sort()));
    }

    Map<String, Object> createValue(String event, Object value) {
        Map<String, Object> values = new HashMap<>();
        values.put("event", event);
        values.put("data", value);
        return values;
    }

    Flux<MeasurementValue> fromRealTime(String deviceId) {
        Subscription subscription = Subscription.of(
            "realtime-device-events-measurement",
            "/device/" + productId + "/" + deviceId + "/message/event/*",
            Subscription.Feature.local, Subscription.Feature.broker
        );
        return
            eventBus
                .subscribe(subscription, DeviceMessage.class)
                .filter(EventMessage.class::isInstance)
                .cast(EventMessage.class)
                .map(kv -> SimpleMeasurementValue.of(createValue(kv.getEvent(), kv.getData()), System.currentTimeMillis()))
            ;
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"));

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
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("event");
            property.setName("事件");
            property.setValueType(new StringType());

            SimplePropertyMetadata value = new SimplePropertyMetadata();
            value.setId("data");
            value.setName("数据");
            value.setValueType(new StringType());

            return new ObjectType()
                .addPropertyMetadata(property)
                .addPropertyMetadata(value);
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
            return Mono
                .justOrEmpty(parameter.getString("deviceId"))
                .flatMapMany(deviceId -> {
                    int history = parameter.getInt("history").orElse(0);
                    return //合并历史数据和实时数据
                        Flux.concat(
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
