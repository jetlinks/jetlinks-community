/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.device.measurements.status;

import lombok.Generated;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class DeviceStatusChangeMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final TimeSeriesManager timeSeriesManager;

    private final DeviceRegistry deviceRegistry;

    static MeasurementDefinition definition = MeasurementDefinition.of("change", "设备状态变更");

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"));

    static DataType type = new EnumType()
        .addElement(EnumType.Element.of(MessageType.OFFLINE.name().toLowerCase(), "离线"))
        .addElement(EnumType.Element.of(MessageType.ONLINE.name().toLowerCase(), "在线"));

    public DeviceStatusChangeMeasurement(TimeSeriesManager timeSeriesManager,
                                         EventBus eventBus,
                                         DeviceRegistry deviceRegistry) {
        super(definition);
        this.deviceRegistry = deviceRegistry;
        this.eventBus = eventBus;
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new RealTimeDeviceStateDimension());
        addDimension(new CountDeviceStateDimension());
    }

    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("type", "类型", "上线or离线", new EnumType()
            .addElement(EnumType.Element.of("online", "上线"))
            .addElement(EnumType.Element.of("offline", "离线")))
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType());

    static DataType historyValueType = new IntType();

    /**
     * 设备状态统计
     */
    class CountDeviceStateDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        @Generated
        public DataType getValueType() {
            return historyValueType;
        }

        @Override
        @Generated
        public ConfigMetadata getParams() {
            return historyConfigMetadata;
        }

        @Override
        @Generated
        public boolean isRealTime() {
            return false;
        }

        private AggregationQueryParam createQueryParam(MeasurementParameter parameter) {
            String format = parameter.getString("format").orElse("yyyy年MM月dd日");
            return AggregationQueryParam
                .of()
                .sum("count")
                .groupBy(parameter.getInterval("time", null), format)
                .filter(query ->
                            query.where("name", parameter.getString("type").orElse("online"))
                                 .is("productId", parameter.getString("productId").orElse(null))
                )
                .limit(parameter.getInt("limit").orElse(1))
                .from(parameter
                          .getDate("from")
                          .orElse(Date.from(LocalDateTime
                                                .now()
                                                .plusDays(-1)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant())))
                .to(parameter.getDate("to").orElse(new Date()));
        }

        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {
            String format = parameter.getString("format").orElse("yyyy年MM月dd日");
            DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
            AggregationQueryParam param = createQueryParam(parameter);

            return Flux
                .defer(() -> param
                    .execute(timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceMetrics())::aggregation)
                    .map(data -> {
                        long ts = data
                            .getString("time")
                            .map(time -> DateTime.parse(time, formatter).getMillis())
                            .orElse(System.currentTimeMillis());
                        return SimpleMeasurementValue.of(
                            data.get("count").orElse(0),
                            data.getString("time", ""),
                            ts);
                    })
                    .sort())
                .take(param.getLimit());
        }
    }

    /**
     * 实时设备变更状态
     */
    class RealTimeDeviceStateDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return type;
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
                .flatMapMany(deviceId ->//从消息网关订阅消息
                    eventBus.subscribe(Subscription.of(
                        "RealTimeDeviceStateDimension"
                        , new String[]{
                            "/device/*/" + deviceId + "/online",
                            "/device/*/" + deviceId + "/offline"
                        },
                        Subscription.Feature.local,
                        Subscription.Feature.broker
                    ), DeviceMessage.class)
                        .map(msg -> SimpleMeasurementValue.of(createStateValue(msg), msg.getTimestamp())));
        }

        Map<String, Object> createStateValue(DeviceMessage message) {
            Map<String, Object> val = new HashMap<>();
            val.put("type", message.getMessageType().name().toLowerCase());
            val.put("deviceId", message.getDeviceId());
            return val;
        }

        MeasurementValue createMeasurementValue(String deviceId, DeviceState state, long timestamp) {
            Map<String, Object> val = new HashMap<>();

            val.put("type", state.getValue());
            val.put("deviceId", deviceId);
            return SimpleMeasurementValue.of(val, timestamp);
        }
    }
}
