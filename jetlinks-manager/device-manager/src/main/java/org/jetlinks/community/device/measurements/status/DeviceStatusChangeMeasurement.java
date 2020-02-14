package org.jetlinks.community.device.measurements.status;

import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

class DeviceStatusChangeMeasurement extends StaticMeasurement {

    public MessageGateway messageGateway;

    private TimeSeriesManager timeSeriesManager;

    static MeasurementDefinition definition = new MeasurementDefinition() {
        @Override
        public String getId() {
            return "device-status";
        }

        @Override
        public String getName() {
            return "设备状态";
        }
    };

    static ConfigMetadata configMetadata = new DefaultConfigMetadata()
        .add("deviceId", "设备", "指定设备", new StringType().expand("selector", "device-selector"));

    static DataType type = new EnumType()
        .addElement(EnumType.Element.of(MessageType.OFFLINE.name().toLowerCase(), "离线"))
        .addElement(EnumType.Element.of(MessageType.ONLINE.name().toLowerCase(), "在线"));

    public DeviceStatusChangeMeasurement(TimeSeriesManager timeSeriesManager, MessageGateway messageGateway) {
        super(definition);
        this.messageGateway = messageGateway;
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new RealTimeDeviceStateDimension());
        addDimension(new HistoryDeviceStateDimension());
    }

    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("type", "类型", "上线or离线", new EnumType()
            .addElement(EnumType.Element.of("online", "上线"))
            .addElement(EnumType.Element.of("offline", "离线")))
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType())
        ;

    static DataType historyValueType = new IntType();

    class HistoryDeviceStateDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.history;
        }

        @Override
        public DataType getValueType() {
            return historyValueType;
        }

        @Override
        public ConfigMetadata getParams() {
            return historyConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {

            return AggregationQueryParam.of()
                .sum("count")
                .groupBy(parameter.getDuration("time").orElse(Duration.ofHours(1)),
                    "time",
                    parameter.getString("format").orElse("MM-dd:HH"))
                .filter(query -> query.where("name", parameter.getString("type").orElse("online")))
                .limit(parameter.getInt("limit").orElse(1))
                .from(parameter.getDate("from").orElse(Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())))
                .to(parameter.getDate("to").orElse(new Date()))
                .execute(timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceMetrics())::aggregation)
                .map(data -> SimpleMeasurementValue.of(
                    data.getInt("count").orElse(0),
                    data.getString("time").orElse(""),
                    System.currentTimeMillis()));
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
                .flatMapMany(deviceId -> {
                    //从消息网关订阅消息
                    return messageGateway
                        .subscribe(Arrays.asList(
                            new Subscription("/device/" + deviceId + "/online"),
                            new Subscription("/device/" + deviceId + "/offline")), true)
                        .flatMap(val -> Mono.justOrEmpty(DeviceMessageUtils.convert(val)))
                        .map(msg -> SimpleMeasurementValue.of(msg.getMessageType().name().toLowerCase(), msg.getTimestamp()))
                        ;
                });

        }
    }
}
