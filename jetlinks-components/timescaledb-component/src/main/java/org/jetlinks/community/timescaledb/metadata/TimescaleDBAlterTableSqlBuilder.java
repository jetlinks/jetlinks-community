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
