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
