package org.jetlinks.community.device.measurements;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.property.Property;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class DevicePropertiesMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final DeviceDataService dataService;

    private final String productId;

    private final DeviceRegistry registry;

    public DevicePropertiesMeasurement(String productId,
                                       EventBus eventBus,
                                       DeviceDataService dataService,
                                       DeviceRegistry registry) {
        super(MeasurementDefinition.of("properties", "属性记录"));
        this.productId = productId;
        this.eventBus = eventBus;
        this.dataService = dataService;
        this.registry = registry;
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

    Map<String, Object> createValue(DeviceMetadata metadata, Property property) {
        return metadata
            .getProperty(property.getId())
            .map(meta -> {
                Map<String, Object> values = new HashMap<>();
                DataType type = meta.getValueType();
                Object val;
                if (type instanceof NumberType) {
                    NumberType<?> numberType = ((NumberType<?>) type);
                    val = NumberType.convertScaleNumber(property.getValue(), numberType.getScale(), numberType.getRound(), Function.identity());
                } else if (type instanceof Converter) {
                    val = ((Converter<?>) type).convert(property.getValue());
                } else {
                    val = property.getValue();
                }
                values.put("formatValue", type.format(val));
                values.put("value", val);
                values.put("state", property.getState());
                values.put("property", property.getId());
                values.put("timestamp",property.getTimestamp());
                if (type instanceof UnitSupported) {
                    UnitSupported unitSupported = (UnitSupported) type;
                    values.put("unit", Optional.ofNullable(unitSupported.getUnit())
                                               .map(ValueUnit::getSymbol)
                                               .orElse(null));
                }
                return values;
            })
            .orElseGet(() -> {
                Map<String, Object> values = new HashMap<>();
                values.put("formatValue", property.getValue());
                values.put("value", property.getValue());
                values.put("state", property.getState());
                values.put("property", property.getId());
                values.put("timestamp",property.getTimestamp());
                return values;
            });
    }

    static Subscription.Feature[] clusterFeature = {Subscription.Feature.local, Subscription.Feature.broker};
    static Subscription.Feature[] nonClusterFeature = {Subscription.Feature.local};


    Flux<MeasurementValue> fromRealTime(String deviceId, Set<String> properties, boolean cluster) {

        Subscription subscription = Subscription.of(
            "realtime-device-properties-measurement",
            new String[]{
                "/device/" + productId + "/" + deviceId + "/message/property/report",
                "/device/" + productId + "/" + deviceId + "/message/property/*/reply"
            },
            cluster ? clusterFeature : nonClusterFeature
        );
        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMapMany(metadata -> {
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
                        .flatMap(msg -> Flux
                            .fromIterable(DeviceMessageUtils.tryGetCompleteProperties(msg))
                            .filter(e -> index.containsKey(e.getId()))
                            //对本次上报的属性进行排序
                            .sort(Comparator.comparingInt(e -> index.getOrDefault(e.getId(), 0)))
                            .<MeasurementValue>map(e -> SimpleMeasurementValue.of(createValue(metadata, e), e.getTimestamp())))
                        .onErrorContinue((err, v) -> log.error(err.getMessage(), err))
                    ;
            });
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
        @Generated
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.history;
        }

        @Override
        @Generated
        public DataType getValueType() {
            return new ObjectType()
                .addProperty("property", "属性", StringType.GLOBAL)
                .addProperty("value", "值", StringType.GLOBAL)
                .addProperty("formatValue", "格式化值", StringType.GLOBAL);
        }

        @Override
        @Generated
        public ConfigMetadata getParams() {
            return configMetadata;
        }

        @Override
        @Generated
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
        @Generated
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        @Generated
        public DataType getValueType() {
            return new ObjectType()
                .addProperty("property", "属性", StringType.GLOBAL)
                .addProperty("value", "值", StringType.GLOBAL)
                .addProperty("formatValue", "格式化值", StringType.GLOBAL);
        }

        @Override
        @Generated
        public ConfigMetadata getParams() {
            return configMetadata;
        }

        @Override
        @Generated
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
                    return Flux.concat(
                        //查询历史数据
                        fromHistory(deviceId, history, getPropertiesFromParameter(parameter))
                        ,
                        //从消息网关订阅实时事件消息
                        fromRealTime(deviceId, getPropertiesFromParameter(parameter), parameter.getBoolean("cluster", true))
                    );
                });
        }
    }
}
