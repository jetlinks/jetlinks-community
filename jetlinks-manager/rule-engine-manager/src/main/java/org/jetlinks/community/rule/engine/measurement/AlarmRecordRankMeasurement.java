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
package org.jetlinks.community.rule.engine.measurement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
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

    AlarmHistoryService historyService;

    public AlarmRecordRankMeasurement(AlarmHistoryService historyService) {
        super(MeasurementDefinition.of("rank", "告警记录排名"));
        this.historyService = historyService;
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
            AggregationQueryParam aggregationQueryParam = AggregationQueryParam.of();
            aggregationQueryParam.setTimeProperty("alarmTime");

            return aggregationQueryParam
                    .groupBy(parameter.getString("group", "targetId"))
                    .count("targetId", "count")
                    .agg("targetId", Aggregation.TOP)
                    .agg("targetName", Aggregation.TOP)
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
            if (Objects.equals(parameter.getString("order", ""), "asc")) {
                comparator = Comparator.comparingLong(d -> d.getLong("count", 0L));
            } else {
                comparator = Comparator.<AggregationData>comparingLong(d -> d.getLong("count", 0L)).reversed();
            }

            AggregationQueryParam param = createQueryParam(parameter);
            return param
                    .execute(historyService::aggregation)
                    .groupBy(a -> a.getString("targetId", null))
                    .flatMap(fluxGroup -> fluxGroup.reduce(AggregationData::merge))
                    .sort(comparator)
                    .map(data -> SimpleMeasurementValue.of(new SimpleResult(data), 0))
                    .take(param.getLimit());
        }

        public Flux<SimpleMeasurementValue> getValue(AggregationQueryParam param, Comparator<AggregationData> comparator) {
            return param
                    .execute(historyService::aggregation)
                    .groupBy(a -> a.getString("targetId", null))
                    .flatMap(fluxGroup -> fluxGroup.reduce(AggregationData::merge))
                    .sort(comparator)
                    .map(data -> SimpleMeasurementValue.of(new SimpleResult(data), 0))
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
