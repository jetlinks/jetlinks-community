package org.jetlinks.community.timescaledb.metadata;

import org.hswebframework.ezorm.rdb.executor.DefaultBatchSqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CommonCreateTableSqlBuilder;

public class TimescaleDBCreateTableSqlBuilder extends CommonCreateTableSqlBuilder {

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
            "SELECT add_retention_policy( ? , INTERVAL '" + interval + "')",
            table.getFullName()
        );
    }

    private SqlRequest createCreateHypertableSQL(RDBTableMetadata table, CreateHypertable createHypertable) {

        String interval = createHypertable.getChunkTimeInterval().getNumber().intValue() + " "
            + createHypertable.getChunkTimeInterval().getUnit().name().toLowerCase();

        return SqlRequests.of(
            "SELECT create_hypertable( ? , ? , chunk_time_interval => INTERVAL '" + interval + "')",
            table.getFullName(),
            table.getColumnNow(createHypertable.getColumn()).getName()
        );
    }
}
