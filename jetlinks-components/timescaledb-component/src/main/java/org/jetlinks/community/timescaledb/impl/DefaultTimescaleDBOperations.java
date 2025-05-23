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
package org.jetlinks.community.timescaledb.impl;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSyncSqlExecutor;
import org.hswebframework.ezorm.rdb.metadata.RDBDatabaseMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.dialect.Dialect;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.DefaultDatabaseOperator;
import org.jetlinks.community.datasource.rdb.RDBDataSource;
import org.jetlinks.community.datasource.rdb.RDBDataSourceProperties;
import org.jetlinks.community.datasource.rdb.RDBDataSourceProvider;
import org.jetlinks.community.timescaledb.TimescaleDBDataWriter;
import org.jetlinks.community.timescaledb.TimescaleDBOperations;
import org.jetlinks.community.timescaledb.TimescaleDBProperties;
import org.jetlinks.community.timescaledb.metadata.TimescaleDBDialectProvider;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import reactor.core.Disposable;
import reactor.core.Disposables;

import javax.annotation.Nonnull;
import java.util.Map;

@RequiredArgsConstructor
public class DefaultTimescaleDBOperations implements TimescaleDBOperations, ApplicationContextAware, CommandLineRunner {

    private final TimescaleDBProperties properties;
    private final Disposable.Composite disposable = Disposables.composite();

    private ApplicationContext context;
    private DatabaseOperator database;
    private DefaultTimescaleDBDataWriter writer;


    public void shutdown() {
        disposable.dispose();
    }

    public void init() {
        if (properties.isSharedSpring() && context != null) {
            //使用spring共享数据源
            ReactiveSqlExecutor sqlExecutor = context.getBean(ReactiveSqlExecutor.class);
            RDBDatabaseMetadata database = new RDBDatabaseMetadata(Dialect.POSTGRES);
            database.addFeature(sqlExecutor);
            database.addFeature(ReactiveSyncSqlExecutor.of(sqlExecutor));

            RDBSchemaMetadata schema = TimescaleDBDialectProvider.GLOBAL.createSchema(properties.getSchema());
            database.addSchema(schema);
            database.setCurrentSchema(schema);
            this.database = DefaultDatabaseOperator.of(database);
        } else {
            if (properties.getR2dbc() == null) {
                throw new IllegalArgumentException("timescaledb.r2dbc must not be null");
            }
            RDBDataSourceProperties datasource = new RDBDataSourceProperties();
            datasource.setType(RDBDataSourceProperties.Type.r2dbc);
            datasource.setSchema(properties.getSchema());
            datasource.setUsername(properties.getR2dbc().getUsername());
            datasource.setPassword(properties.getR2dbc().getPassword());
            datasource.setUrl(properties.getR2dbc().getUrl());
            datasource.setDialect(TimescaleDBDialectProvider.NAME);

            Map<String, Object> others = Maps.newHashMap();
            others.put("properties", properties.getR2dbc().getProperties());
            others.put("pool", properties.getR2dbc().getPool());

            datasource.setOthers(others);

            RDBDataSource dataSource = RDBDataSourceProvider
                .create("TimescaleDB", datasource);
            disposable.add(dataSource);
            database = dataSource.operator();
        }
        writer = new DefaultTimescaleDBDataWriter(database, properties.getWriteBuffer());
        writer.init();
        disposable.add(writer::stop);
    }

    @Override
    public DatabaseOperator database() {
        return database;
    }

    @Override
    public TimescaleDBDataWriter writer() {
        return writer;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public void run(String... args) {
        if (writer != null) {
            SpringApplication
                .getShutdownHandlers()
                .add(writer::shutdown);
            writer.start();
        }
    }
}
