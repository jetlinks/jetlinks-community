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

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.ddl.CreateTableSqlBuilder;
import org.jetlinks.community.tdengine.TDengineConstants;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
@Getter
@Setter
public class TDengineCreateTableSqlBuilder implements CreateTableSqlBuilder {

    @Override
    public SqlRequest build(RDBTableMetadata table) {
        PrepareSqlFragments sql = PrepareSqlFragments.of();

        List<String> columns = new ArrayList<>(table.getColumns().size());
        sql.addSql("CREATE STABLE IF NOT EXISTS", table.getFullName(), "(")
            .addSql("_ts timestamp");


        List<RDBColumnMetadata> tags = new ArrayList<>();
        for (RDBColumnMetadata column : table.getColumns()) {
            if (column.getProperty(TDengineConstants.COLUMN_IS_TS).isTrue()) {
                continue;
            }
            if (column.getProperty(TDengineConstants.COLUMN_IS_TAG).isTrue()) {
                tags.add(column);
                continue;
            }
            sql
                .addSql(",")
                .addSql(column.getQuoteName())
                .addSql(column.getDataType());

        }
        sql.addSql(")");
        if(!tags.isEmpty()){
            sql.addSql("TAGS (");
            int index= 0 ;
            for (RDBColumnMetadata tag : tags) {
                if(index++>0){
                    sql.addSql(",");
                }
                sql
                    .addSql(tag.getQuoteName())
                    .addSql(tag.getDataType());
            }
            sql.addSql(")");
        }
        return sql.toRequest();
    }

}
