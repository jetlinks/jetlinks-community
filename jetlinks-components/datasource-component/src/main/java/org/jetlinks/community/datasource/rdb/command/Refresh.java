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
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.community.datasource.DataSourceConstants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class Refresh implements RDBCommand<Mono<Void>> {

    @Override
    public Mono<Void> execute(DatabaseOperator operator) {
        return Flux
            .fromIterable(operator
                .getMetadata()
                .getSchemas())
            .concatMap(RDBSchemaMetadata::loadAllTableReactive)
            .then();
    }

    public static FunctionMetadata metadata() {
        return DataSourceConstants.Metadata
            .create(Refresh.class, func -> {
                func.setName("刷新RDB数据源信息");
            });
    }
}
