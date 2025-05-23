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
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.metadata.parser.TableMetadataParser;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class GetTable implements RDBCommand<Mono<Table>> {

    private final String name;

    @Override
    public Mono<Table> execute(DatabaseOperator operator) {
        return operator
            .getMetadata()
            .getCurrentSchema()
            .<TableMetadataParser>findFeatureNow(TableMetadataParser.id)
            .parseByNameReactive(name)
            .cast(TableOrViewMetadata.class)
            .map(Table::of);
    }
}
