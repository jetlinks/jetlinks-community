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
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class CreateOrAlterTable implements RDBCommand<Mono<Void>> {

    private final Table table;

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {

        TableBuilder builder = operator
            .ddl()
            .createOrAlter(table.getName());

        for (Column column : table.getColumns()) {
            builder
                .addColumn(column.getName())
                .custom(rdb -> {
                    rdb.setPrimaryKey(column.isPrimaryKey());
                    rdb.setNotNull(column.isNotnull());
                    rdb.setPreviousName(column.getPreviousName());
                })
                .length(column.getLength(), column.getScale())
                .type(column.getType())
                .comment(column.getComment())
                .commit();
        }

        return builder
            .commit()
            .reactive()
            .then();
    }
}
