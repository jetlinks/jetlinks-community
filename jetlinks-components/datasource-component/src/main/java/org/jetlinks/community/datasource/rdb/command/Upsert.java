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

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
public class Upsert implements RDBCommand<Mono<Void>> {
    private final String table;

    private final List<Map<String, Object>> dataList;

    private final Set<String> ignoreUpdateColumn;

    public Upsert(String table, List<Map<String, Object>> dataList) {
        this(table, dataList, Collections.emptySet());
    }

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {
        return operator
            .getMetadata()
            .getTableReactive(table)
            .flatMap(tableMetadata -> operator
                .dml()
                .upsert(tableMetadata)
                .values(dataList)
                .ignoreUpdate(ignoreUpdateColumn == null ? new String[0] : ignoreUpdateColumn.toArray(new String[0]))
                .execute()
                .reactive()
                .then()
            );
    }
}
