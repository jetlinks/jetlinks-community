package org.jetlinks.community.device.measurements;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
class DevicePropertiesMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final DeviceMetadata metadata;

    private final DeviceDataService dataService;

    private final String productId;

    public DevicePropertiesMeasurement(String productId,
                                       EventBus eventBus,
                                       DeviceDataService dataService,
                                       DeviceMetadata deviceMetadata) {
        super(MeasurementDefinition.of("properties", "属性记录"));
        this.productId = productId;
        this.eventBus = eventBus;
        this.metadata = deviceMetadata;
        this.dataService = dataService;
        addDimension(new RealTimeDevicePropertyDimension());
        addDimension(new HistoryDevicePropertyDimension());

    }

    Flux<SimpleMeasurementValue> fromHistory(String deviceId, int history, Set<String> properties) {
        return history <= 0
            ? Flux.empty()
            : QueryParamEntity
            .newQuery()
            .doPaging(0, history)
            .execute(q -> dataService.queryEachProperties(deviceId, q, properties.toArray(new String[0])))
            .map(data -> SimpleMeasurementValue.of(data, data.getTimestamp()))
            .sort(MeasurementValue.sort());
    }

    Map<String, Object> createValue(String property, Object value) {
        return metadata
            .getProperty(property)
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

    Flux<MeasurementValue> fromRealTime(String deviceId, Set<String> properties) {

        Subscription subscription = Subscription.of(
            "realtime-device-properties-measurement",
            new String[]{
                "/device/" + productId + "/" + deviceId + "/message/property/report",
                "/device/" + productId + "/" + deviceId + "/message/property/*/reply"
            },
            Subscription.Feature.local, Subscription.Feature.broker
        );
        List<PropertyMetadata> props = metadata.getProperties();
        Map<String, Integer> index = new HashMap<>();
        int idx = 0;
        for (PropertyMetadata prop : props) {
            if (properties.isEmpty() || properties.contains(prop.getId())) {
                index.put(prop.getId(), idx++);
            }
        }
        return
            eventBus
                .subscribe(subscription, DeviceMessage.class)
                .flatMap(msg -> Mono.justOrEmpty(DeviceMessageUtils.tryGetProperties(msg)))
                .flatMap(map -> Flux
                    .fromIterable(map.entrySet())
                    //对本次上报的属性进行排序
                    .sort(Comparator.comparingInt(e -> index.getOrDefault(e.getKey(), 0))))
                .<MeasurementValue>map(kv -> SimpleMeasurementValue.of(createValue(kv.getKey(), kv.getValue()), System.currentTimeMillis()))
                .onErrorContinue((err, v) -> log.error(err.getMessage(), err))
            ;
    }

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"));

    static Set<String> getPropertiesFromParameter(MeasurementParameter parameter) {
        return parameter
            .get("properties")
            .map(CastUtils::castArray)
            .orElse(Collections.emptyList())
            .stream()
            .map(String::valueOf)
            .collect(Collectors.toSet());
    }

    /**
     * 历史
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
                .addProperty("value", "值", StringType.GLOBAL)
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
            return Mono
                .justOrEmpty(parameter.getString("deviceId"))
                .flatMapMany(deviceId -> {
                    int history = parameter.getInt("history").orElse(1);

                    return fromHistory(deviceId, history, getPropertiesFromParameter(parameter));
                });
        }
    }

    /**
     * 实时
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
                .addProperty("value", "值", StringType.GLOBAL)
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
            return Mono
                .justOrEmpty(parameter.getString("deviceId"))
                .flatMapMany(deviceId -> {
                    int history = parameter.getInt("history").orElse(0);
                    //合并历史数据和实时数据
                    return  Flux.concat(
                        //查询历史数据
                        fromHistory(deviceId, history, getPropertiesFromParameter(parameter))
                        ,
                        //从消息网关订阅实时事件消息
                        fromRealTime(deviceId, getPropertiesFromParameter(parameter))
                    );
                });
        }
    }
}
