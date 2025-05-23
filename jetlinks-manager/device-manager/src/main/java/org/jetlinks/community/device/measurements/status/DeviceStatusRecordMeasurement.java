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
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.Interval;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.reactor.ql.ReactorQL;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

class DeviceStatusRecordMeasurement
    extends StaticMeasurement {

    public final ReactiveRepository<DeviceInstanceEntity,String> deviceRepository;

    private final TimeSeriesManager timeSeriesManager;

    static MeasurementDefinition definition = MeasurementDefinition.of("record", "设备状态记录");

    public DeviceStatusRecordMeasurement(ReactiveRepository<DeviceInstanceEntity,String> deviceRepository,
                                         TimeSeriesManager timeSeriesManager) {
        super(definition);
        this.timeSeriesManager = timeSeriesManager;
        this.deviceRepository = deviceRepository;
        addDimension(new CurrentNumberOfDeviceDimension());
        addDimension(new AggNumberOfOnlineDeviceDimension());
    }

    static ConfigMetadata aggConfigMetadata = new DefaultConfigMetadata()
        .add("productId", "设备型号", "", new StringType())
        .add("time", "周期", "例如: 1h,10m,30s", new StringType())
        .add("format", "时间格式", "如: MM-dd:HH", new StringType())
        .add("limit", "最大数据量", "", new IntType())
        .add("from", "时间从", "", new DateTimeType())
        .add("to", "时间至", "", new DateTimeType());


    //历史在线数量
    class AggNumberOfOnlineDeviceDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return DimensionDefinition.of("aggOnline", "历史在线数");
        }

        @Override
        @Generated
        public DataType getValueType() {
            return new IntType();
        }

        @Override
        @Generated
        public ConfigMetadata getParams() {
            return aggConfigMetadata;
        }

        @Override
        @Generated
        public boolean isRealTime() {
            return false;
        }

        ReactorQL ql = ReactorQL
            .builder()
            .sql("select time,sum(value) value from dual group by time")
            .build();

        //select time,sum(value) value from dual group by time
        @Override
        public Flux<SimpleMeasurementValue> getValue(MeasurementParameter parameter) {
            String format = parameter.getString("format").orElse("yyyy年MM月dd日");
            DateTimeFormatter formatter = DateTimeFormat.forPattern(format);

            return AggregationQueryParam
                .of()
                .max("value")
                .filter(query -> query.where("name", "gateway-server-session"))
                .from(parameter
                          .getDate("from")
                          .orElse(Date.from(LocalDateTime
                                                .now()
                                                .plusDays(-30)
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant())))
                .to(parameter.getDate("to").orElse(new Date()))
                .groupBy(parameter.getInterval("time").orElse(Interval.ofDays(1)), format)
                .groupBy("server")
                .limit(parameter.getInt("limit").orElse(10))
                .execute(timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceMetrics())::aggregation)
                .map(AggregationData::asMap)
                .as(ql::start)
                .map(data -> {
                    String timeStr = String.valueOf(data.get("time"));
                    long ts = DateTime.parse(timeStr, formatter).getMillis();
                    return SimpleMeasurementValue.of(
                        data.getOrDefault("value", 0),
                        timeStr,
                        ts);
                })
                .sort()
                .take(parameter.getLong("limit").orElse(10L));
        }
    }

    static ConfigMetadata currentMetadata = new DefaultConfigMetadata()
        .add("productId", "设备型号", "", new StringType())
        .add("state", "状态", "online", new EnumType()
            .addElement(EnumType.Element.of(DeviceState.online.getValue(), DeviceState.online.getText()))
            .addElement(EnumType.Element.of(DeviceState.offline.getValue(), DeviceState.offline.getText()))
            .addElement(EnumType.Element.of(DeviceState.notActive.getValue(), DeviceState.notActive.getText()))
        );

    //当前设备数量
    class CurrentNumberOfDeviceDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.current;
        }

        @Override
        @Generated
        public DataType getValueType() {
            return new IntType();
        }

        @Override
        @Generated
        public ConfigMetadata getParams() {
            return currentMetadata;
        }

        @Override
        @Generated
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Mono<MeasurementValue> getValue(MeasurementParameter parameter) {
            return deviceRepository
                .createQuery()
                .and(DeviceInstanceEntity::getProductId, parameter.getString("productId").orElse(null))
                .and(DeviceInstanceEntity::getState, parameter.get("state", DeviceState.class).orElse(null))
                .count()
                .map(val -> SimpleMeasurementValue.of(val, System.currentTimeMillis()));
        }
    }


}
