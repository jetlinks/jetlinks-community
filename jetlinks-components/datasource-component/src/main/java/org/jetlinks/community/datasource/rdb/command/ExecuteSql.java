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
package org.jetlinks.community.datasource.rdb.command;

import lombok.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.executor.wrapper.ColumnWrapperContext;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.query.DefaultQueryHelper;
import org.hswebframework.web.crud.query.QueryAnalyzer;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.datasource.DataSourceConstants;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class ExecuteSql extends AbstractCommand<Flux<ExecuteSql.SqlResult<?>>, ExecuteSql>
    implements RDBCommand<Flux<ExecuteSql.SqlResult<?>>> {

    private static final String PARAMETERSTRING = "sqlRequests";

    public List<ExecuteSqlRequest> getSqlRequests() {
        return ConverterUtils.convertToList(readable().getOrDefault(PARAMETERSTRING, new ExecuteSqlRequest()),
            params -> FastBeanCopier.copy(params, new ExecuteSqlRequest()));
    }

    @Override
    public Flux<SqlResult<?>> execute(DatabaseOperator operator) {

        return Flux.fromIterable(getSqlRequests())
            .flatMap(sqlRequest -> {
                String sql = sqlRequest.getSql();
                Assert.hasText(sql, "'sql' can not be empty");
                SqlRequest finalRealSql = SqlRequests.template(sql, sqlRequest.getParameter());
                Statement statement = parseSql(finalRealSql.getSql());
                if (statement instanceof Select) {
                    return operator
                        .sql()
                        .reactive()
                        .select(finalRealSql, new SqlResultWrapper())
                        .defaultIfEmpty(parseSelect(finalRealSql.getSql(), operator))
                        .take(1000)
                        .collectList()
                        .map(list -> SqlResult.of(list, Operation.select.name()));
                }
                return operator
                    .sql()
                    .reactive()
                    .update(finalRealSql)
                    .map(updateCount -> SqlResult.of(updateCount, Operation.upsert.name()));
            });
    }

    @AllArgsConstructor
    static class SqlResultWrapper implements ResultWrapper<LinkedHashMap<String, Object>, Void> {

        private final Map<String, Integer> columnCountMap = new ConcurrentHashMap<>();

        @Override
        public LinkedHashMap<String, Object> newRowInstance() {
            return new LinkedHashMap<>();
        }

        @Override
        public void wrapColumn(ColumnWrapperContext<LinkedHashMap<String, Object>> context) {

            String column = context.getColumnLabel();
            Object value = String.valueOf(context.getResult());

            columnCountMap.compute(column, (key, count) -> {
                if (context.getRowInstance().containsKey(key)) {
                    count = (count == null) ? 1 : count + 1;
                    StringBuilder sb = new StringBuilder(column)
                        .append("(")
                        .append(count)
                        .append(")");
                    context.getRowInstance().put(sb.toString(), value);
                } else {
                    context.getRowInstance().put(key, value);
                    count = 0;
                }
                return count;
            });
        }

        @Override
        public boolean completedWrapRow(LinkedHashMap<String, Object> result) {
            if (!columnCountMap.isEmpty()) {
                columnCountMap.clear();
            }
            return true;
        }

        @Override
        public Void getResult() {
            return null;
        }
    }


    private LinkedHashMap<String, Object> parseSelect(String sql, DatabaseOperator operator) {

        QueryAnalyzer.Select select = new DefaultQueryHelper(operator)
            .analysis(sql)
            .select();

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();

        select.getColumnList().forEach(column -> result.put(column.getName(), "(N/A)"));

        return result;
    }

    @SneakyThrows
    public Statement parseSql(String sql) {
        return CCJSqlParserUtil.parse(sql);
    }

    public static FunctionMetadata metadata(Consumer<SimpleFunctionMetadata> custom) {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        //ExecuteSql
        metadata.setId("ExecuteSql");
        metadata.setName("执行SQL语句");
        metadata.setInputs(Collections.singletonList(
            SimplePropertyMetadata
                .of(
                    PARAMETERSTRING,
                    "SQL查询参数",
                    new ArrayType().elementType(
                        new ObjectType()
                            .addProperty("sql", "sql语句", new StringType().expand(ConfigMetadataConstants.required,
                                true))
                            .addProperty("parameter", "替换参数", new ObjectType()))
                )
        ));
        custom.accept(metadata);
        return metadata;
    }

    public static FunctionMetadata metadata() {
        return DataSourceConstants.Metadata
            .create(ExecuteSql.class, func -> {
                func.setName("执行SQL语句");
                func.setInputs(Collections.singletonList(
                    SimplePropertyMetadata
                        .of(
                            PARAMETERSTRING,
                            "SQL查询参数",
                            new ArrayType().elementType(
                                new ObjectType()
                                    .addProperty("sql", "sql语句", new StringType().expand(ConfigMetadataConstants.required,
                                        true))
                                    .addProperty("parameter", "替换参数", new ObjectType()))
                        )
                ));
            });
    }

    private enum Operation {
        select,
        upsert
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class SqlResult<T> {

        private T data;

        private String type;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecuteSqlRequest implements Serializable {

        private String sql;

        private Object parameter;

    }

}


