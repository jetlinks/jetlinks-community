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
package org.jetlinks.community.datasource.rdb;

import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import lombok.SneakyThrows;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSyncSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.metadata.RDBDatabaseMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.DefaultDatabaseOperator;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.configuration.DialectProvider;
import org.hswebframework.web.crud.query.DefaultQueryHelper;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.sql.DefaultR2dbcExecutor;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.datasource.rdb.command.*;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.community.datasource.AbstractDataSource;
import org.jetlinks.community.datasource.DataSourceState;
import org.jetlinks.community.datasource.DataSourceType;
import org.jetlinks.community.datasource.rdb.command.*;
import org.jetlinks.community.utils.ObjectMappers;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.r2dbc.connection.ConnectionFactoryUtils;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class DefaultRDBDataSource extends AbstractDataSource<RDBDataSourceProperties> implements RDBDataSource {

    private DatabaseOperator operator;

    private String validateSql;

    private final QueryHelper queryHelper;

    private final List<Closeable> closeables = new CopyOnWriteArrayList<>();

    private final Sinks.One<Void> loading = Sinks.one();

    private TransactionalOperator transactionalOperator;

    public DefaultRDBDataSource(String id,
                                RDBDataSourceProperties config) {
        super(id, config);
        init();
        this.queryHelper = new DefaultQueryHelper(operator);

        //刷新RDB数据源命令：Refresh
        registerHandler(
            Refresh.class,
            CommandHandler.of(
                Refresh.metadata(),
                (cmd, ignore) -> cmd.execute(operator),
                Refresh::new
            )
        );

        //执行SQL命令：ExecuteSql
        registerHandler(
            ExecuteSql.class,
            CommandHandler.of(
                ExecuteSql.metadata(),
                (cmd, ignore) -> loading
                    .asMono()
                    .thenMany(cmd.execute(operator)),
                ExecuteSql::new
            )
        );

        //执行列表查询命令：QueryList
        registerHandler(
            QueryList.class,
            CommandHandler.of(
                QueryList.metadata(),
                (cmd, ignore) -> loading
                    .asMono()
                    .thenMany(cmd.execute(operator)),
                QueryList::new
            )
        );

        //执行分页查询命令：QueryPager
        registerHandler(
            QueryPager.class,
            CommandHandler.of(
                QueryPager.metadata(),
                (cmd, ignore) -> loading
                    .asMono()
                    .then(cmd.execute(operator)),
                QueryPager::new
            )
        );

        //执行统计数量命令：Count
        registerHandler(
            Count.class,
            CommandHandler.of(
                Count.metadata(),
                (cmd, ignore) -> loading
                    .asMono()
                    .then(cmd.execute(operator)),
                Count::new
            )
        );

    }

    private void loadTables() {
        new Refresh().execute(operator)
                     .doOnTerminate(loading::tryEmitEmpty)
                     .subscribe();
    }

    @Override
    public DatabaseOperator operator() {
        return operator;
    }

    @Override
    public QueryHelper helper() {
        return queryHelper;
    }

    @Override
    public DataSourceType getType() {
        return RDBDataSourceType.rdb;
    }

    @SneakyThrows
    public void init() {
        try {
            if (getConfig().getType() == RDBDataSourceProperties.Type.r2dbc) {
                initR2dbc();
            } else {
                initJdbc();
            }
            loadTables();
            validateSql = getConfig().getValidateQuery();
        } catch (Throwable e) {
            throw translateException(e);
        }
    }

    @SneakyThrows
    synchronized void initR2dbc() {
        DialectProvider dialect = getConfig().dialectProvider();
        R2dbcProperties properties;
        if (MapUtils.isNotEmpty(getConfig().getOthers())) {
            //使用jsonCopy,FastBeanCopier不支持final字段copy.
            properties = ObjectMappers
                .parseJson(ObjectMappers.toJsonBytes(getConfig().getOthers()), R2dbcProperties.class);
        } else {
            properties = new R2dbcProperties();
        }
        properties.setUrl(getConfig().getUrl());
        properties.setUsername(getConfig().getUsername());
        properties.setPassword(getConfig().getPassword());

        PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();

        ConnectionFactoryBuilder connectionFactoryBuilder = ConnectionFactoryBuilder.withUrl(properties.getUrl());

        mapper.from(properties.getUsername()).whenNonNull().to(connectionFactoryBuilder::username);
        mapper.from(properties.getPassword()).whenNonNull().to(connectionFactoryBuilder::password);

        R2dbcProperties.Pool pool = properties.getPool();
        ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactoryBuilder.build());
        builder.maxLifeTime(Duration.ofMinutes(10));
        builder.maxAcquireTime(Duration.ofSeconds(10));
        mapper.from(pool.getMaxIdleTime()).to(builder::maxIdleTime);
        mapper.from(pool.getInitialSize()).to(builder::initialSize);
        mapper.from(pool.getMaxSize()).to(builder::maxSize);
        mapper.from(pool.getValidationQuery()).whenHasText().to(builder::validationQuery);
        mapper.from(pool.getMaxLifeTime()).whenNonNull().to(builder::maxLifeTime);
        mapper.from(pool.getMaxAcquireTime()).whenNonNull().to(builder::maxAcquireTime);
        mapper.from(pool.getMaxLifeTime()).whenNonNull().to(builder::maxLifeTime);
        mapper.from(pool.getMaxAcquireTime()).whenNonNull().to(builder::maxAcquireTime);
        mapper.from(pool.getValidationQuery()).whenHasText().to(builder::validationQuery);

        ConnectionPool connectionPool = new ConnectionPool(builder.build());
        closeables.add(() -> connectionPool.close().subscribe());
        transactionalOperator = TransactionalOperator.create(new R2dbcTransactionManager(connectionPool));

        DefaultR2dbcExecutor executor = new DefaultR2dbcExecutor() {

            @Override
            protected Mono<Connection> getConnection() {
                return ConnectionFactoryUtils.getConnection(connectionPool);
            }

            @Override
            public Mono<Void> execute(Publisher<SqlRequest> request) {
                return super
                    .execute(request)
                    .as(transactionalOperator::transactional);
            }

            @Override
            public Mono<Integer> update(Publisher<SqlRequest> request) {
                return super
                    .update(request)
                    .as(transactionalOperator::transactional);
            }

            @Override
            public <E> Flux<E> select(Publisher<SqlRequest> request, ResultWrapper<E, ?> wrapper) {
                return super
                    .select(request, wrapper)
                    .as(transactionalOperator::transactional);
            }
        };
        executor.setDefaultFactory(connectionPool);
        executor.setBindSymbol(dialect.getBindSymbol());
        executor.setBindCustomSymbol(!executor.getBindSymbol().equals("?"));
        RDBDatabaseMetadata database = new RDBDatabaseMetadata(dialect.getDialect());
        database.addFeature(executor);
        database.addFeature(ReactiveSyncSqlExecutor.of(executor));

        RDBSchemaMetadata schema = dialect.createSchema(getConfig().getSchema());
        database.addSchema(schema);
        database.setCurrentSchema(schema);
        this.operator = DefaultDatabaseOperator.of(database);

    }

    private Throwable translateException(Throwable err) {
        if (err instanceof IllegalStateException) {
            if (err.getMessage() != null && err.getMessage().contains("Available drivers")) {
                return new I18nSupportException("error.unsupported_database_type", err);
            }
        }
        if (err instanceof DataAccessResourceFailureException) {
            return new I18nSupportException("error.database_access_error", err);
        }
        if (err instanceof SQLException) {
            String msg = err.getMessage();
            if (msg.contains("No suitable driver")) {
                return new I18nSupportException("error.unsupported_database_type", err);
            }
            return err;
        }
        if (err.getClass() == RuntimeException.class) {
            if (err.getCause() != null && err.getCause() != err) {
                return translateException(err.getCause());
            }
        }
        return err;
    }

    synchronized void initJdbc() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(getConfig().getUrl());
        dataSource.setUsername(getConfig().getUsername());
        dataSource.setPassword(getConfig().getPassword());
        if (MapUtils.isNotEmpty(getConfig().getOthers())) {
            FastBeanCopier.copy(getConfig().getOthers(), dataSource);
        }
        closeables.add(dataSource);
        RDBDatabaseMetadata database = new RDBDatabaseMetadata(getConfig().dialectProvider().getDialect());
        database.addFeature(new RDBJdbcReactiveSqlExecutor(dataSource));
        database.addFeature(new RDBJdbcSyncSqlExecutor(dataSource));

        RDBSchemaMetadata schema = getConfig().dialectProvider().createSchema(getConfig().getSchema());
        database.addSchema(schema);
        database.setCurrentSchema(schema);

        this.operator = DefaultDatabaseOperator.of(database);
        this.transactionalOperator = null;
    }

    @Override
    protected Mono<DataSourceState> checkState() {
        return operator
            .sql()
            .reactive()
            .select(validateSql)
            .map(i -> DataSourceState.ok)
            .onErrorResume(err -> Mono.just(DataSourceState.error(translateException(err))))
            .last();
    }

    @Override
    @SuppressWarnings("all")
    protected <R> R executeUndefinedCommand(@Nonnull Command<R> command) {
        if (command instanceof RDBCommand) {
            R r = ((RDBCommand<R>) command).execute(operator);
            if (transactionalOperator != null) {
                if (r instanceof Mono) {
                    return (R) transactionalOperator.transactional(((Mono) r));
                } else if (r instanceof Flux) {
                    return (R) transactionalOperator.transactional(((Flux) r));
                }
            }
            return r;
        }
        return super.executeUndefinedCommand(command);
    }

    @Override
    protected void handleSetConfig(RDBDataSourceProperties oldConfig,
                                   RDBDataSourceProperties newConfig) {
        if (oldConfig == null) {
            return;
        }
        releaseOld();
        init();
    }

    protected void releaseOld() {
        closeables.removeIf(closeable -> {
            try {
                closeable.close();
            } catch (Throwable ignore) {
            }
            return true;
        });
    }

    @Override
    protected void doOnDispose() {
        releaseOld();
    }
}
