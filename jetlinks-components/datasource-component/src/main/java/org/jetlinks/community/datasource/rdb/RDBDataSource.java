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

import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.community.datasource.rdb.command.*;
import org.jetlinks.community.datasource.DataSource;
import org.jetlinks.community.datasource.DataSourceType;
import org.jetlinks.core.command.Command;

import javax.annotation.Nonnull;


public interface RDBDataSource extends DataSource {

    RDBDataSourceProperties getConfig();

    RDBDataSourceProperties copyConfig();

    DatabaseOperator operator();

    QueryHelper helper();

    @Nonnull
    @Override
    default <R> R execute(@Nonnull Command<R> command) {
        return DataSource.super.execute(command);
    }

    @Override
    default DataSourceType getType() {
        return RDBDataSourceType.rdb;
    }
}
