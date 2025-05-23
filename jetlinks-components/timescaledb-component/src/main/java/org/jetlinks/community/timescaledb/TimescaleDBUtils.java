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
package org.jetlinks.community.timescaledb;

import org.hswebframework.ezorm.rdb.mapping.defaults.record.Record;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.dml.query.NativeSelectColumn;
import org.hswebframework.ezorm.rdb.operator.dml.query.SelectColumn;
import org.hswebframework.ezorm.rdb.supports.postgres.JsonbType;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.Interval;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.jetlinks.community.timescaledb.metadata.JsonbValueCodec;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.reactor.ql.utils.CastUtils;

public class TimescaleDBUtils {


    public static String getTableName(String name) {
        return ThingsDatabaseUtils.createTableName(name);
    }

    public static NativeSelectColumn createTimeGroupColumn(long startWith, Interval interval) {

        String unit = interval.getNumber().intValue() + " " + interval
            .getUnit()
            .name()
            .toLowerCase();

        return NativeSelectColumn
            .of("time_bucket('" + unit + "',timestamp)");
    }

    public static TimeSeriesData convertToTimeSeriesData(Record record) {
        return TimeSeriesData.of(
            record.get(ThingsDataConstants.COLUMN_TIMESTAMP)
                  .map(val -> CastUtils.castNumber(val).longValue())
                  .orElseGet(System::currentTimeMillis),
            record
        );
    }

    public static void customColumn(PropertyMetadata metadata, RDBColumnMetadata column) {
        DataType type = metadata.getValueType();

        if (type instanceof ArrayType) {
            column.setType(new JsonbType());
            column.setValueCodec(new JsonbValueCodec(true));
        } else if (type instanceof ObjectType) {
            column.setType(new JsonbType());
            column.setValueCodec(new JsonbValueCodec(false));
        }

    }


    public static void applyAggColumn(Aggregation aggregation, SelectColumn column) {
        String function;
        switch (aggregation) {
            case COUNT:
                function = "count";
                break;
            case AVG:
                function = "avg";
                break;
            case FIRST:
            case TOP:
            case MAX:
                function = "max";
                break;
            case LAST:
            case MIN:
                function = "min";
                break;
            case SUM:
                function = "sum";
                break;
            case DISTINCT_COUNT:
                function = "count";
                column.option("distinct", true);
                break;
            default:
                throw new UnsupportedOperationException("不支持的聚合函数:" + aggregation);
        }
        column.setFunction(function);
    }


}
