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

import org.hswebframework.ezorm.rdb.executor.wrapper.MapResultWrapper;
import org.hswebframework.ezorm.rdb.operator.DMLOperator;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.query.DefaultQueryHelper;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.datasource.DataSourceConstants;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


public class QueryPager extends QueryPagerCommand<Map<String, Object>>
    implements RDBCommand<Mono<PagerResult<Map<String, Object>>>> {

    private final static String PAGING_COLUMN = "ROWNUM_";

    public String getTable() {
        return String.valueOf(readable().get("table"));
    }

    public String getSql() {
        return String.valueOf(readable().get("sql"));
    }

    public QueryParamEntity getParam() {
        return this.asQueryParam();
    }

    @Override
    public Mono<PagerResult<Map<String, Object>>> execute(DatabaseOperator operator) {

        QueryParamEntity param = getParam();
        String table = getTable();
        String sql = getSql();
        DMLOperator dmlOperator = operator.dml();

        DefaultQueryHelper queryHelper = new DefaultQueryHelper(operator);

        if (sql.isEmpty() || sql.equals("null")) {
            return Mono.zip(
                dmlOperator
                    .query(table)
                    .setParam(param)
                    .paging(param.getPageIndex(), param.getPageSize())
                    .fetch(new MapResultWrapper())
                    .reactive()
                    .map(map -> {
                        map.remove(PAGING_COLUMN);
                        return map;
                    })
                    .collectList(),
                dmlOperator
                    .createReactiveRepository(table)
                    .createQuery()
                    .setParam(param)
                    .count(),
                (list, total) -> PagerResult.of(total, list, param));
        }
        return QueryHelper
            .transformPageResult(
                queryHelper
                    .select(sql)
                    .where(param)
                    .fetchPaged(),
                list -> Flux
                    .fromIterable(list)
                    .map(record -> (Map<String, Object>)record)
                    .collectList()
            );
    }

    public static FunctionMetadata metadata() {
        List<PropertyMetadata> list = getInputList();
        return DataSourceConstants.Metadata
            .create(QueryPager.class, func -> {
                func.setName("分页查询");
                func.setInputs(list);
            });
    }

    public static List<PropertyMetadata> getInputList() {
        List<PropertyMetadata> list = new ArrayList<>(QueryPagerCommand.getQueryParamMetadata());
        list.add(SimplePropertyMetadata.of("table", "表名", StringType.GLOBAL));
        list.add(SimplePropertyMetadata.of("sql", "sql语句", StringType.GLOBAL));
        return list;
    }

    public static <T> CommandHandler<QueryPager, Mono<PagerResult<Map<String, Object>>>> createQueryHandler(
        Consumer<SimpleFunctionMetadata> custom,
        Function<QueryPager, Mono<PagerResult<Map<String, Object>>>> handler) {
        return CommandHandler.of(
            () -> metadata(custom),
            (cmd, ignore) -> handler.apply(cmd),
            QueryPager::new
        );

    }

    public static FunctionMetadata metadata(Consumer<SimpleFunctionMetadata> custom) {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        metadata.setId("QueryPager");
        metadata.setName("分页查询");
        metadata.setInputs(getInputList());
        custom.accept(metadata);
        return metadata;
    }
}
