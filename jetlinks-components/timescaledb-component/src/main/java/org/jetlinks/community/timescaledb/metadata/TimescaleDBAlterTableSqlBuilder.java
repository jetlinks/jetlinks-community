package org.jetlinks.community.timescaledb.metadata;

import org.hswebframework.ezorm.rdb.executor.DefaultBatchSqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.metadata.RDBIndexMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CommonCreateTableSqlBuilder;
import org.hswebframework.ezorm.rdb.supports.postgres.PostgresqlAlterTableSqlBuilder;

public class TimescaleDBAlterTableSqlBuilder extends PostgresqlAlterTableSqlBuilder {


    @Override
    protected void appendDropIndexSql(DefaultBatchSqlRequest batch, RDBTableMetadata table, RDBIndexMetadata index) {

    }

    @Override
    protected void appendAddIndexSql(DefaultBatchSqlRequest batch, RDBTableMetadata table, RDBIndexMetadata index) {

    }
}
