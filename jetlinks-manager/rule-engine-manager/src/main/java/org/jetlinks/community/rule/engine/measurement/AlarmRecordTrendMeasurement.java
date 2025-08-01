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

import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author bestfeng
 */
public class AlarmRecordTrendMeasurement extends StaticMeasurement {

    AlarmHistoryService historyService;

    public AlarmRecordTrendMeasurement(AlarmHistoryService historyService) {
        super(MeasurementDefinition.of("trend", "告警记录趋势"));
        this.historyService = historyService;
        addDimension(new AggRecordTrendDimension());
    }


    static ConfigMetadata aggConfigMetadata = new DefaultConfigMetadata()
            .add("alarmConfigId", "告警配置Id", "", StringType.GLOBAL)
            .add("time", "周期", "例如: 1h,10m,30s", StringType.GLOBAL)
            .add("agg", "聚合类型", "count,sum,avg,max,min", StringType.GLOBAL)
            .add("format", "时间格式", "如: MM-dd:HH", StringType.GLOBAL)
            .add("limit", "最大数据量", "", StringType.GLOBAL)
            .add("from", "时间从", "", StringType.GLOBAL)
            .add("to", "时间至", "", StringType.GLOBAL);


    class AggRecordTrendDimension implements MeasurementDimension {

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
                    .groupBy(parameter.getInterval("time", null),
                             parameter.getString("format").orElse("MM月dd日 HH时"))
                    .count("targetId", "count")
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
            return param
                    .execute(historyService::aggregation)
                    .index((index, data) -> SimpleMeasurementValue.of(
                            data.getLong("count", 0),
                            data.getString("time", ""),
                            index))
                    .take(param.getLimit());
        }

    }
}
