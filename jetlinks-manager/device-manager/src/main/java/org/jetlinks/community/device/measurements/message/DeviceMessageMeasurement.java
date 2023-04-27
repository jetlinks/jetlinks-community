package org.jetlinks.community.device.measurements.message;

import lombok.Generated;
import org.jetlinks.community.Interval;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

class DeviceMessageMeasurement extends StaticMeasurement {

    private final EventBus eventBus;

    private final TimeSeriesManager timeSeriesManager;

    static MeasurementDefinition definition = MeasurementDefinition.of("quantity", "设备消息量");

    public DeviceMessageMeasurement(EventBus eventBus,
                                    TimeSeriesManager timeSeriesManager) {
        super(definition);
        this.eventBus = eventBus;
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new RealTimeMessageDimension());
        addDimension(new AggMessageDimension());
    }

    static ConfigMetadata realTimeConfigMetadata = new DefaultConfigMetadata()
        .add("interval", "数据统计周期", "例如: 1s,10s", new StringType());

    class RealTimeMessageDimension implements MeasurementDimension {

        @Override
        @Generated
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        @Generated
        public DataType getValueType() {
            return IntType.GLOBAL;
        }

        @Override
        @Generated
        public ConfigMetadata getParams() {
            return realTimeConfigMetadata;
        }

        @Override
        @Generated
        public boolean isRealTime() {
            return true;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {


            //通过订阅消息来统计实时数据量
            return eventBus
                .subscribe(Subscription.of("real-time-device-message", "/device/**", Subscription.Feature.local, Subscription.Feature.broker))
                .doOnNext(TopicPayload::release)
                .window(parameter.getDuration("interval").orElse(Duration.ofSeconds(1)))
                .flatMap(Flux::count)
                .map(total -> SimpleMeasurementValue.of(total, System.currentTimeMillis()));
        }
    }

    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("productId", "设备型号", "", new StringType())
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("msgType", "消息类型", "", new StringType())
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType());

    class AggMessageDimension implements MeasurementDimension {


        @Override
        @Generated
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        @Generated
        public DataType getValueType() {
            return IntType.GLOBAL;
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

        public AggregationQueryParam createQueryParam(MeasurementParameter parameter) {
            return AggregationQueryParam
                .of()
                .sum("count")
                .groupBy(parameter.getInterval("interval", parameter.getInterval("time", null)),
                         parameter.getString("format").orElse("MM月dd日 HH时"))
                .filter(query -> query
                    .where("name", "message-count")
                    .is("productId", parameter.getString("productId").orElse(null))
                )
                .limit(parameter.getInt("limit").orElse(1))
                .from(parameter
                          .getDate("from")
                          .orElseGet(() -> Date
                              .from(LocalDateTime
                                        .now()
                                        .plusDays(-1)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant())))
                .to(parameter.getDate("to").orElse(new Date()));
        }

        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {
            AggregationQueryParam param = createQueryParam(parameter);

            return Flux.defer(() -> param
                .execute(timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceMetrics())::aggregation)
                .index((index, data) -> SimpleMeasurementValue.of(
                    data.getLong("count",0),
                    data.getString("time").orElse(""),
                    index)))
                .take(param.getLimit());
        }
    }

}
