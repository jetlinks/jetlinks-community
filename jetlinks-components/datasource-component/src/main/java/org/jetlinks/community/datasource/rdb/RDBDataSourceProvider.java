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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.query.DefaultQueryHelper;
import org.hswebframework.web.crud.query.QueryAnalyzer;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.community.datasource.DataSource;
import org.jetlinks.community.datasource.DataSourceConfig;
import org.jetlinks.community.datasource.DataSourceProvider;
import org.jetlinks.community.datasource.DataSourceType;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.community.datasource.*;
import org.jetlinks.community.datasource.rdb.command.QueryList;
import org.jetlinks.community.datasource.rdb.command.QueryPager;
import org.jetlinks.community.datasource.rdb.command.RDBRequestListCommand;
import org.jetlinks.community.datasource.rdb.command.RDBRequestPagerCommand;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;

@Component
public class RDBDataSourceProvider implements DataSourceProvider {

    @Nonnull
    @Override
    public DataSourceType getType() {
        return RDBDataSourceType.rdb;
    }

    @Nonnull
    @Override
    public Mono<DataSource> createDataSource(@Nonnull DataSourceConfig properties) {

        return Mono
            .<DataSource>fromCallable(() -> RDBDataSourceProvider
                .create(properties.getId(),
                    FastBeanCopier.copy(properties.getConfiguration(), new RDBDataSourceProperties())
                        .validate()
                )
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Nonnull
    @Override
    public Mono<DataSource> reload(@Nonnull DataSource dataSource, @Nonnull DataSourceConfig properties) {
        return Mono
            .defer(() -> {
                RDBDataSourceProperties dataSourceProperties = FastBeanCopier
                    .copy(properties.getConfiguration(), RDBDataSourceProperties::new)
                    .validate();
                if (dataSource.isWrapperFor(DefaultRDBDataSource.class)) {
                    dataSource
                        .unwrap(DefaultRDBDataSource.class)
                        .setConfig(dataSourceProperties);
                    return Mono.just(dataSource);
                }
                dataSource.dispose();
                return createDataSource(properties);
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    public static RDBDataSource create(String id, RDBDataSourceProperties properties) {
        return new DefaultRDBDataSource(id, properties);
    }


    @Override
    public Mono<CommandHandler<?, ?>> createCommandHandler(CommandConfiguration configuration) {
        Configuration config = FastBeanCopier.copy(configuration.getConfiguration(), Configuration.class);
        RDBDefinition rdbDefinition = config.getRdbDefinition();
        Boolean paging = rdbDefinition.getPaging();

        return getRdbDataSource(configuration)
            .flatMap(source -> {
                List<PropertyMetadata> resultColumns = getResultColumns(
                    rdbDefinition.getSql(),
                    source.helper(),
                    configuration.getMonitor());
                if (paging) {
                    return Mono.just(
                        RDBRequestPagerCommand
                            .createQueryHandler(
                                configuration.getCommandId(),
                                configuration.getCommandName(),
                                metadata -> metadata.setOutput(QueryPagerCommand.createOutputType(resultColumns)),
                                cmd -> getRdbDataSource(configuration)
                                    .flatMap(database -> database.execute(new QueryPager().with(cmd.with("sql", rdbDefinition.getSql()).asMap())))
                            )
                    );
                }
                return Mono.just(
                    RDBRequestListCommand
                        .createQueryHandler(
                            configuration.getCommandId(),
                            configuration.getCommandName(),
                            metadata -> metadata.setOutput(getResultType(resultColumns)),
                            cmd -> getRdbDataSource(configuration)
                                .flatMapMany(database -> database.execute(new QueryList().with(cmd.with("sql", rdbDefinition.getSql()).asMap())))
                ));
            });

    }

    private static Mono<RDBDataSource> getRdbDataSource(CommandConfiguration configuration) {
        return configuration
            .getDataSource()
            .map(datasource -> datasource.unwrap(RDBDataSource.class));
    }

    private Flux<Map<String, Object>> queryList(String sql, DatabaseOperator operator, QueryParamEntity param) {
        QueryHelper queryHelper = new DefaultQueryHelper(operator);
        return queryHelper
            .select(sql)
            .where(param)
            .fetch()
            .map(Function.identity());
    }

    private DataType getResultType(List<PropertyMetadata> propertyMetadata) {
        ObjectType type = new ObjectType();
        type.setProperties(propertyMetadata);
        return type;
    }

    private List<PropertyMetadata> getResultColumns(String sql, QueryHelper queryHelper, Monitor monitor) {
        try {
            QueryAnalyzer _analyzer = queryHelper.analysis(sql);
            return parseColumToProperties(_analyzer);
        } catch (Exception e) {
            monitor
                .logger()
                .error("初始化sql查询失败", e);
        }
        return Collections.emptyList();
    }

    static List<PropertyMetadata> parseColumToProperties(QueryAnalyzer analyzer) {
        List<PropertyMetadata> columns = new ArrayList<>();

        QueryAnalyzer.Table table = analyzer.select().getTable();

        Map<String, List<PropertyMetadata>> nests = new LinkedHashMap<>();

        for (QueryAnalyzer.Column column : analyzer.select().getColumnList()) {
            DataType type;
            String name;
            RDBColumnMetadata metadata = column.getMetadata();
            if (metadata == null) {
                type = StringType.GLOBAL;
                name = column.getName();
            } else {
                type = ThingsDatabaseUtils.convertDataType(column.getMetadata());
                name = column.getMetadata().getComment();
            }

            if (Objects.equals(column.getOwner(), table.getAlias())) {
                columns.add(SimplePropertyMetadata.of(column.getAlias(), name, type));
            } else {
                nests.computeIfAbsent(column.getOwner(), __ -> new ArrayList<>())
                    .add(SimplePropertyMetadata.of(column.getAlias(), name, type));
            }
        }
        for (Map.Entry<String, List<PropertyMetadata>> nest : nests.entrySet()) {
            ObjectType type = new ObjectType();
            type.setProperties(nest.getValue());
            columns.add(SimplePropertyMetadata.of(nest.getKey(), nest.getKey(), type));
        }

        return columns;
    }

    @Getter
    @Setter
    public static class Configuration {

        private RDBDefinition rdbDefinition;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RDBDefinition {

        @Schema(description = "sql语句")
        private String sql;

        @Schema(description = "是否分页")
        private Boolean paging;

        @Schema(description = "其他配置")
        private Map<String, Object> others;
    }
}
