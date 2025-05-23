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
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.query.DefaultQueryHelper;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.datasource.DataSourceConstants;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class QueryList extends QueryListCommand<Map<String, Object>>
    implements RDBCommand<Flux<Map<String, Object>>>{

    public Pair<String, String> getSqlOrTable() {
        return Optional.ofNullable(readable().get("sql"))
            .map(sql -> Pair.of("sql", String.valueOf(sql)))
            .orElse(Optional.ofNullable(readable().get("table"))
                .map(table -> Pair.of("table", String.valueOf(table)))
                .orElse(null));
    }

    @Override
    public Flux<Map<String, Object>> execute(DatabaseOperator operator) {

        if (getSqlOrTable() == null) {
            return Flux.error(new UnsupportedOperationException("sql or table is not found"));
        }

        QueryParamEntity param = this.asQueryParam();
        DefaultQueryHelper queryHelper = new DefaultQueryHelper(operator);

        if (getSqlOrTable().getFirst().equals("sql")) {
            return queryHelper
                .select(getSqlOrTable().getSecond())
                .where(param)
                .fetch()
                .map(Function.identity());
        }

        if (getSqlOrTable().getFirst().equals("table")) {
            return operator
                .dml()
                .query(getSqlOrTable().getSecond())
                .setParam(param)
                .fetch(new MapResultWrapper())
                .reactive();
        }
        return Flux.empty();
    }

    public static FunctionMetadata metadata() {
        List<PropertyMetadata> list = getInputList();
        return DataSourceConstants.Metadata
            .create(QueryList.class, func -> {
                func.setName("列表查询");
                func.setInputs(list);
            });
    }

    public static List<PropertyMetadata> getInputList() {
        List<PropertyMetadata> list = new ArrayList<>(QueryListCommand.getQueryParamMetadata());
        list.add(SimplePropertyMetadata.of("table", "表名", StringType.GLOBAL));
        list.add(SimplePropertyMetadata.of("sql", "sql语句", StringType.GLOBAL));
        return list;
    }
}
