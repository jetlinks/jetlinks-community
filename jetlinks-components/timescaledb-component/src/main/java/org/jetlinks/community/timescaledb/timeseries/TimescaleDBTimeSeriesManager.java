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
package org.jetlinks.community.timescaledb.timeseries;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.codec.DateTimeCodec;
import org.hswebframework.ezorm.rdb.metadata.RDBIndexMetadata;
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.Interval;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.jetlinks.community.timescaledb.TimescaleDBOperations;
import org.jetlinks.community.timescaledb.TimescaleDBUtils;
import org.jetlinks.community.timescaledb.metadata.CreateHypertable;
import org.jetlinks.community.timescaledb.metadata.CreateRetentionPolicy;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import java.sql.JDBCType;
import java.util.Date;

import static org.jetlinks.community.ConfigMetadataConstants.indexEnabled;

@Slf4j
@AllArgsConstructor
public class TimescaleDBTimeSeriesManager implements TimeSeriesManager {

    private final TimescaleDBTimeSeriesProperties properties;

    private final TimescaleDBOperations operations;

    @Override
    public TimeSeriesService getService(TimeSeriesMetric metric) {
        return getService(metric.getId());
    }

    @Override
    public TimeSeriesService getServices(TimeSeriesMetric... metric) {
        if (metric.length == 1) {
            return getServices(metric[0].getId());
        }
        throw new UnsupportedOperationException("unsupported multi metric");
    }

    @Override
    public TimeSeriesService getServices(String... metric) {
        if (metric.length == 1) {
            return getService(metric[0]);
        }
        throw new UnsupportedOperationException("unsupported multi metric");
    }

    @Override
    public TimeSeriesService getService(String metric) {
        return new TimescaleDBTimeSeriesService(TimescaleDBUtils.getTableName(metric), operations);
    }

    @Override
    public Mono<Void> registerMetadata(TimeSeriesMetadata metadata) {
        String tableName = TimescaleDBUtils.getTableName(metadata.getMetric().getId());
        TableBuilder builder = operations
            .database()
            .ddl()
            .createOrAlter(tableName)
            .custom(table -> {
                table.addFeature(new CreateHypertable(ThingsDataConstants.COLUMN_TIMESTAMP, properties.getChunkTimeInterval()));
                Interval interval = properties.getRetentionPolicy(tableName);
                if (interval != null && interval.getNumber().longValue() > 0) {
                    table.addFeature(new CreateRetentionPolicy(interval));
                }
            });
        for (PropertyMetadata property : metadata.getProperties()) {
            builder
                .addColumn(property.getId())
                .custom(column -> {
                    ThingsDatabaseUtils.convertColumn(property, column);
                    TimescaleDBUtils.customColumn(property, column);
                })
                .commit();
            //开启索引
            if (property.getExpand(indexEnabled).orElse(false)) {
                builder
                    .index()
                    .name("idx_" + tableName + "_" + property.getId())
                    .column(property.getId())
                    .commit();
            }
        }
        builder
            .addColumn(ThingsDataConstants.COLUMN_ID)
            .comment("ID")
            .varchar(64)
            .notNull()
            .commit();

        builder
            .addColumn(ThingsDataConstants.COLUMN_TIMESTAMP)
            .comment("时间戳")
            .custom(column -> {
                //column
                column.setJdbcType(JDBCType.TIMESTAMP, Date.class);
                column.setValueCodec(new DateTimeCodec("yyyy-MM-dd HH:mm:ss.SSS", Date.class));
            })
            .notNull()
            .commit();

        builder
            .index()
            .name("idx_" + tableName + "_id")
            .column(ThingsDataConstants.COLUMN_ID)
            .column(ThingsDataConstants.COLUMN_TIMESTAMP, RDBIndexMetadata.IndexSort.desc)
            .unique()
            .commit();

        return builder
            .commit()
            .reactive()
            .then()
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }


}
