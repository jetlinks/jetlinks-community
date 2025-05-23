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
package org.jetlinks.community.tdengine.metadata;

import org.hswebframework.ezorm.rdb.executor.DefaultBatchSqlRequest;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CommonAlterTableSqlBuilder;
import org.jetlinks.community.tdengine.TDengineConstants;

import static org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments.of;

public class TDengineAlterTableSqlBuilder extends CommonAlterTableSqlBuilder {

    @Override
    protected void appendAddColumnCommentSql(DefaultBatchSqlRequest batch, RDBColumnMetadata column) {

    }

    protected PrepareSqlFragments createAlterTable(RDBColumnMetadata column) {
        return of()
            .addSql("ALTER", "STABLE", column.getOwner().getFullName());
    }

    @Override
    protected void appendAddColumnSql(DefaultBatchSqlRequest batch, RDBColumnMetadata column) {

        if (column.getProperty(TDengineConstants.COLUMN_IS_TS).isTrue()) {
            return;
        }
        PrepareSqlFragments fragments = createAlterTable(column);

        fragments
            .addSql("ADD", column.getProperty(TDengineConstants.COLUMN_IS_TAG).isTrue() ? "COLUMN" : "TAG")
            .addSql(column.getName())
            .addSql(column.getDataType());

        batch.addBatch(fragments.toRequest());
    }

    @Override
    protected void appendDropColumnSql(DefaultBatchSqlRequest batch, RDBColumnMetadata drop) {
        if (drop.getProperty(TDengineConstants.COLUMN_IS_TS).isTrue()) {
            return;
        }
        PrepareSqlFragments fragments = createAlterTable(drop);
        fragments.addSql("DROP",drop.getProperty(TDengineConstants.COLUMN_IS_TAG).isTrue() ? "COLUMN" : "TAG")
            .addSql(drop.getName());

        batch.addBatch(fragments.toRequest());
    }

    @Override
    protected void appendAlterColumnSql(DefaultBatchSqlRequest batch,
                                        RDBColumnMetadata oldColumn,
                                        RDBColumnMetadata newColumn) {
        if (newColumn.getProperty(TDengineConstants.COLUMN_IS_TS).isTrue()) {
            return;
        }

        PrepareSqlFragments fragments = createAlterTable(newColumn);
        fragments.addSql("MODIFY",newColumn.getProperty(TDengineConstants.COLUMN_IS_TAG).isTrue() ? "COLUMN" : "TAG")
            .addSql(newColumn.getName())
            .addSql(newColumn.getDataType());

        batch.addBatch(fragments.toRequest());
    }
}
