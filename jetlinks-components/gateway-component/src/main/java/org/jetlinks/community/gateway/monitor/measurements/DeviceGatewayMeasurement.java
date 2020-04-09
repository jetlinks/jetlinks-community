package org.jetlinks.community.gateway.monitor.measurements;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.Interval;
import org.jetlinks.community.timeseries.query.Aggregation;
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

        private TimeSeriesManager timeSeriesManager;

        private String type;

        private Aggregation defaultAgg;

        private String property;

    public DeviceGatewayMeasurement(MeasurementDefinition definition,
                                    String property,
                                    Aggregation defaultAgg,
                                    TimeSeriesManager timeSeriesManager) {
        super(definition);
        this.timeSeriesManager = timeSeriesManager;
        this.defaultAgg = defaultAgg;
        this.type = definition.getId();
        this.property = property;
        addDimension(new AggDeviceStateDimension());
        addDimension(new HistoryDimension());
    }

    static ConfigMetadata historyConfigMetadata = new DefaultConfigMetadata()
        .add("gatewayId", "网关", "", new StringType())
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType().format("yyyy-MM-dd HH:mm:ss"))
        .add("to", "时间至", "", new DateTimeType().format("yyyy-MM-dd HH:mm:ss"));


    class HistoryDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.history;
        }

        @Override
        public DataType getValueType() {
            return new IntType();
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
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {
            return QueryParamEntity.newQuery()
                .where("target", type)
                .is("name", parameter.getString("gatewayId").orElse(null))
                .doPaging(0, parameter.getInt("limit").orElse(1))
                .between("timestamp",
                    parameter.getDate("from").orElseGet(() -> Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())),
                    parameter.getDate("to").orElseGet(Date::new)
                )
                .execute(timeSeriesManager.getService(GatewayTimeSeriesMetric.deviceGatewayMetric())::query)
                .map(data -> SimpleMeasurementValue.of(
                    data.getInt(property).orElse(0),
                    data.getTimestamp()))
                .sort(MeasurementValue.sort());
        }
    }


    static ConfigMetadata aggConfigMetadata = new DefaultConfigMetadata()
        .add("gatewayId", "网关", "", new StringType())
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("agg", "聚合方式", "", new EnumType()
            .addElement(EnumType.Element.of("SUM", "总和"))
            .addElement(EnumType.Element.of("MAX", "最大值"))
            .addElement(EnumType.Element.of("MIN", "最小值"))
            .addElement(EnumType.Element.of("AVG", "平局值"))
        )
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType().format("yyyy-MM-dd HH:mm:ss"))
        .add("to", "时间至", "", new DateTimeType().format("yyyy-MM-dd HH:mm:ss"));


    //聚合数据
    class AggDeviceStateDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        public DataType getValueType() {
            return new IntType();
        }

        @Override
        public ConfigMetadata getParams() {
            return aggConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {

            return AggregationQueryParam.of()
                .agg(property, parameter.get("agg", Aggregation.class).orElse(defaultAgg))
                .groupBy(parameter.getInterval("time").orElse(Interval.ofHours(1)),
                    "time",
                    parameter.getString("format").orElse("MM-dd:HH"))
                .filter(query -> query
                    .where("target", type)
                    .is("name", parameter.getString("gatewayId").orElse(null)))
                .limit(parameter.getInt("limit").orElse(1))
                .from(parameter.getDate("from").orElseGet(() -> Date.from(LocalDateTime.now().plusDays(-1).atZone(ZoneId.systemDefault()).toInstant())))
                .to(parameter.getDate("to").orElse(new Date()))
                .execute(timeSeriesManager.getService(GatewayTimeSeriesMetric.deviceGatewayMetric())::aggregation)
                .index((index, data) -> SimpleMeasurementValue.of(
                    data.getInt(property).orElse(0),
                    data.getString("time").orElse(""),
                    index))
                .sort();
        }
    }

}
