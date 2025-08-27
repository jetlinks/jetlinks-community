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
package org.jetlinks.community.timescaledb.metadata;

import org.hswebframework.ezorm.rdb.executor.DefaultBatchSqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CommonCreateTableSqlBuilder;

public class TimescaleDBCreateTableSqlBuilder extends CommonCreateTableSqlBuilder {

    private String schema;

    public TimescaleDBCreateTableSqlBuilder(String schema) {
        this.schema = schema;
    }

    @Override
    public SqlRequest build(RDBTableMetadata table) {
        DefaultBatchSqlRequest sqlRequest = (DefaultBatchSqlRequest) super.build(table);

        table.getFeature(CreateHypertable.ID)
             .ifPresent(createHypertable -> sqlRequest.addBatch(createCreateHypertableSQL(table, createHypertable)));

        table.getFeature(CreateRetentionPolicy.ID)
             .ifPresent(feature -> sqlRequest.addBatch(createCreateRetentionPolicySQL(table, feature)));

        return sqlRequest;
    }


    private SqlRequest createCreateRetentionPolicySQL(RDBTableMetadata table, CreateRetentionPolicy createHypertable) {

        String interval = createHypertable.getInterval().getNumber().intValue() + " "
            + createHypertable.getInterval().getUnit().name().toLowerCase();

        return SqlRequests.of(
            "SELECT "+ schema +".add_retention_policy( ? , INTERVAL '" + interval + "')",
            table.getFullName()
        );
    }

    private SqlRequest createCreateHypertableSQL(RDBTableMetadata table, CreateHypertable createHypertable) {

        String interval = createHypertable.getChunkTimeInterval().getNumber().intValue() + " "
            + createHypertable.getChunkTimeInterval().getUnit().name().toLowerCase();

        return SqlRequests.of(
            "SELECT "+ schema +".create_hypertable( ? , ? , chunk_time_interval => INTERVAL '" + interval + "')",
            table.getFullName(),
            table.getColumnNow(createHypertable.getColumn()).getName()
        );
    }
}
