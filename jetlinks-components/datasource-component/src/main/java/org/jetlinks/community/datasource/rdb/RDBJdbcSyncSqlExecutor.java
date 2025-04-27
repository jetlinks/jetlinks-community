package org.jetlinks.community.datasource.rdb;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.jdbc.JdbcSyncSqlExecutor;

import javax.sql.DataSource;
import java.sql.Connection;

@AllArgsConstructor
public class RDBJdbcSyncSqlExecutor extends JdbcSyncSqlExecutor {
   private final DataSource dataSource;

    @Override
    @SneakyThrows
    public Connection getConnection(SqlRequest sqlRequest) {
        return dataSource.getConnection();
    }

    @Override
    @SneakyThrows
    public void releaseConnection(Connection connection, SqlRequest sqlRequest) {
        connection.close();
    }
}
