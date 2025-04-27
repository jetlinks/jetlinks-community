package org.jetlinks.community.datasource.rdb;

import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.community.datasource.rdb.command.*;
import org.jetlinks.community.datasource.DataSource;
import org.jetlinks.community.datasource.DataSourceType;
import org.jetlinks.core.command.Command;

import javax.annotation.Nonnull;


public interface RDBDataSource extends DataSource {

    RDBDataSourceProperties getConfig();

    RDBDataSourceProperties copyConfig();

    DatabaseOperator operator();

    QueryHelper helper();

    @Nonnull
    @Override
    default <R> R execute(@Nonnull Command<R> command) {
        return DataSource.super.execute(command);
    }

    @Override
    default DataSourceType getType() {
        return RDBDataSourceType.rdb;
    }
}
