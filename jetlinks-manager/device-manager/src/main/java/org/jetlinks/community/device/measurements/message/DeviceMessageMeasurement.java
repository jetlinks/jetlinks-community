package org.jetlinks.community.device.measurements.message;

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

class DeviceMessageMeasurement extends StaticMeasurement {

    private MessageGateway messageGateway;

    private TimeSeriesManager timeSeriesManager;

    static MeasurementDefinition definition = new MeasurementDefinition() {
        @Override
        public String getId() {
            return "device-message-quantity";
        }

        @Override
        public String getName() {
            return "设备消息量";
        }
    };

    public DeviceMessageMeasurement(MessageGateway messageGateway, TimeSeriesManager timeSeriesManager) {
        super(definition);
        this.messageGateway = messageGateway;
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new RealTimeMessageDimension());
        addDimension(new AggMessageDimension());

    }

    static DataType valueType = new IntType();

    static ConfigMetadata realTimeConfigMetadata = new DefaultConfigMetadata()
        .add("interval", "数据统计周期", "例如: 1s,10s", new StringType())
        ;

    class RealTimeMessageDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return valueType;
        }

        @Override
        public ConfigMetadata getParams() {
            return realTimeConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            //通过订阅消息来统计实时数据量
            return messageGateway
                .subscribe("/device/**")
                .window(parameter.getDuration("interval").orElse(Duration.ofSeconds(1)))
                .flatMap(Flux::count)
                .map(total -> SimpleMeasurementValue.of(total, System.currentTimeMillis()));
        }
    }


    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType())
        ;

    class AggMessageDimension implements MeasurementDimension {


        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        public DataType getValueType() {
            return valueType;
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
                .filter(query -> query.where("name", "message-count"))
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

}
