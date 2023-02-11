package org.jetlinks.community.rule.engine.measurement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

/**
 * @author bestfeng
 */
public class AlarmRecordRankMeasurement extends StaticMeasurement {

    TimeSeriesManager timeSeriesManager;

    public AlarmRecordRankMeasurement(TimeSeriesManager timeSeriesManager) {
        super(MeasurementDefinition.of("rank", "告警记录排名"));
        this.timeSeriesManager = timeSeriesManager;
        addDimension(new AggRecordRankDimension());
    }


    static ConfigMetadata aggConfigMetadata = new DefaultConfigMetadata()
        .add("time", "周期", "例如: 1h,10m,30s", StringType.GLOBAL)
        .add("agg", "聚合类型", "count,sum,avg,max,min", StringType.GLOBAL)
        .add("format", "时间格式", "如: MM-dd:HH", StringType.GLOBAL)
        .add("limit", "最大数据量", "", StringType.GLOBAL)
        .add("from", "时间从", "", StringType.GLOBAL)
        .add("to", "时间至", "", StringType.GLOBAL);


    class AggRecordRankDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
        }

        @Override
        public DataType getValueType() {
            return IntType.GLOBAL;
        }

        @Override
        public ConfigMetadata getParams() {
            return aggConfigMetadata;
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        public AggregationQueryParam createQueryParam(MeasurementParameter parameter) {
            return AggregationQueryParam
                .of()
                .groupBy(parameter.getString("group", "targetId"))
                .sum("count", "count")
                .agg("targetId", Aggregation.TOP)
                .filter(query -> query
                    .where("name", "record-agg")
                    .where("targetType", parameter.getString("targetType", null))
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

            Comparator<AggregationData> comparator;
            if (Objects.equals(parameter.getString("order",""), "asc")){
                 comparator = Comparator.comparingLong(d-> d.getLong("count", 0L));
            }else {
                comparator = Comparator.<AggregationData>comparingLong(d-> d.getLong("count", 0L)).reversed();
            }

            AggregationQueryParam param = createQueryParam(parameter);

            return Flux.defer(() -> param
                .execute(timeSeriesManager.getService(AlarmTimeSeriesMetric.alarmStreamMetrics())::aggregation)
                .groupBy(a -> a.getString("targetId", null))
                .flatMap(fluxGroup -> fluxGroup.reduce(AggregationData::merge))
                .sort(comparator)
                .map(data -> SimpleMeasurementValue.of(new SimpleResult(data), 0))
            )
                .take(param.getLimit());
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        class SimpleResult {

            private String targetId;

            private String targetName;

            private long count;

            public SimpleResult(AggregationData data) {
                String targetId = data.getString("targetId", "");
                this.setCount(data.getLong("count", 0L));
                this.setTargetName(data.getString("targetName", targetId));
                this.setTargetId(data.getString("targetId", ""));
            }
        }
    }
}
