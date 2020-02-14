package org.jetlinks.community.gateway.monitor.measurements;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.gateway.monitor.GatewayTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

class DeviceGatewayMeasurement extends StaticMeasurement {

    static MeasurementDefinition definition = new MeasurementDefinition() {
        @Override
        public String getId() {
            return "device-gateway-message-quantity";
        }

        @Override
        public String getName() {
            return "设备网关数据量";
        }
    };

    private TimeSeriesManager timeSeriesManager;

    public DeviceGatewayMeasurement(TimeSeriesManager timeSeriesManager) {
        super(definition);
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new AggDeviceStateDimension());
    }

    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("type", "类型", "", new EnumType()
            .addElement(EnumType.Element.of("connected", "创建连接数"))
            .addElement(EnumType.Element.of("rejected", "拒绝连接数"))
            .addElement(EnumType.Element.of("disconnected", "断开连接数"))
            .addElement(EnumType.Element.of("receivedMessage", "接收消息数"))
            .addElement(EnumType.Element.of("sentMessage", "发送消息数"))
        )
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType().format("yyyy-MM-dd HH:mm:ss"))
        .add("to", "时间至", "", new DateTimeType().format("yyyy-MM-dd HH:mm:ss"));

    static DataType historyValueType = new IntType();

    class AggDeviceStateDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
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
                .filter(query -> query.where("target", parameter.getString("type").orElse("connected")))
                .limit(parameter.getInt("limit").orElse(1))
                .from(parameter.getDate("from").orElse(Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())))
                .to(parameter.getDate("to").orElse(new Date()))
                .execute(timeSeriesManager.getService(GatewayTimeSeriesMetric.deviceGatewayMetric())::aggregation)
                .map(data -> SimpleMeasurementValue.of(
                    data.getInt("count").orElse(0),
                    data.getString("time").orElse(""),
                    System.currentTimeMillis()));
        }
    }

}
