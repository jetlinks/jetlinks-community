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
package org.jetlinks.community.timescaledb.thing;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.codec.DateTimeCodec;
import org.hswebframework.ezorm.rdb.metadata.RDBIndexMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeDDLOperationsBase;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.jetlinks.community.timescaledb.TimescaleDBUtils;
import org.jetlinks.community.timescaledb.metadata.CreateHypertable;
import org.jetlinks.community.timescaledb.metadata.CreateRetentionPolicy;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import java.sql.JDBCType;
import java.util.*;

@Slf4j
public class TimescaleDBRowModeDDLOperations extends RowModeDDLOperationsBase {

    private final DatabaseOperator database;
    private final TimescaleDBThingsDataProperties properties;

    static Set<String> ignoreColumn = Sets.newHashSet(
        ThingsDataConstants.COLUMN_ID,
        ThingsDataConstants.COLUMN_MESSAGE_ID,
        ThingsDataConstants.COLUMN_PROPERTY_GEO_VALUE,
        ThingsDataConstants.COLUMN_PROPERTY_OBJECT_VALUE,
        ThingsDataConstants.COLUMN_PROPERTY_ARRAY_VALUE
    );

    public TimescaleDBRowModeDDLOperations(String thingType,
                                           String templateId,
                                           String thingId,
                                           DataSettings settings,
                                           MetricBuilder metricBuilder,
                                           DatabaseOperator database,
                                           TimescaleDBThingsDataProperties properties) {
        super(thingType, templateId, thingId, settings, metricBuilder);
        this.database = database;
        this.properties = properties;
    }

    protected Mono<Void> register0(MetricType metricType,
                                   String metric,
                                   List<PropertyMetadata> properties,
                                   boolean ddl) {
        metric = TimescaleDBUtils.getTableName(metric);
        RDBSchemaMetadata schema = database.getMetadata().getCurrentSchema();
        RDBTableMetadata table = schema.newTable(metric);
        TableBuilder builder = database
            .ddl()
            .createOrAlter(table);


        List<String> partitions = new ArrayList<>();
        partitions.add(ThingsDataConstants.COLUMN_THING_ID);
        for (PropertyMetadata property : properties) {
            if (ignoreColumn.contains(property.getId())) {
                continue;
            }
            builder
                .addColumn(property.getId())
                .custom(column -> {
                    ThingsDatabaseUtils.convertColumn(property, column);
                    if (Objects
                        .equals(ThingsDataConstants.COLUMN_TIMESTAMP, column.getName())) {
                        column.setNotNull(true);
                        column.setJdbcType(JDBCType.TIMESTAMP, Date.class);
                        column.setValueCodec(new DateTimeCodec("yyyy-MM-dd HH:mm:ss.SSS", Date.class));
                    }
                    TimescaleDBUtils.customColumn(property,column);
                })
                .commit();
        }

        if (metricType == MetricType.properties) {
            //索引
            builder
                .index()
                .name("idx_" + metric + "_prop_id")
                .column(metricBuilder.getThingIdProperty())
                .column(ThingsDataConstants.COLUMN_PROPERTY_ID)
                .column(ThingsDataConstants.COLUMN_TIMESTAMP, RDBIndexMetadata.IndexSort.desc)
                .unique()
                .commit();
        } else if (metricType == MetricType.event && settings.getEvent().eventIsAllInOne()) {
            partitions.add(ThingsDataConstants.COLUMN_EVENT_ID);
            //索引
            builder
                .index()
                .name("idx_" + metric + "_event_id")
                .column(metricBuilder.getThingIdProperty())
                .column(ThingsDataConstants.COLUMN_EVENT_ID)
                .column(ThingsDataConstants.COLUMN_TIMESTAMP, RDBIndexMetadata.IndexSort.desc)
                .unique()
                .commit();
        } else if (metricType == MetricType.log) {
            //索引
            builder
                .index()
                .name("idx_" + metric + "_log_id")
                .column(metricBuilder.getThingIdProperty())
                .column(ThingsDataConstants.COLUMN_TIMESTAMP, RDBIndexMetadata.IndexSort.desc)
                .column(ThingsDataConstants.COLUMN_LOG_TYPE)
                .commit();
        }

        table.addFeature(new CreateHypertable(ThingsDataConstants.COLUMN_TIMESTAMP, this.properties.getChunkTimeInterval()));

        if (this.properties.getRetentionPolicy() != null) {
            table.addFeature(new CreateRetentionPolicy(this.properties.getRetentionPolicy()));
        }

        if (ddl) {
            return builder
                .commit()
                .reactive()
                .then()
                .contextWrite(ctx -> ctx.put(Logger.class, log));
        }
        return schema
            .getTableReactive(metric, true)
            .doOnNext(oldTable -> oldTable.replace(table))
            .switchIfEmpty(Mono.fromRunnable(() -> schema.addTable(table)))
            .then()
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    @Override
    protected Mono<Void> register(MetricType metricType, String metric, List<PropertyMetadata> properties) {

        return register0(metricType, metric, properties, true);
    }

    @Override
    protected Mono<Void> reload(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return register0(metricType, metric, properties, false);
    }
}
