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
package org.jetlinks.community.datasource;

import reactor.core.publisher.Flux;
import org.jetlinks.community.datasource.exception.DataSourceNotExistException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 数据源管理器,用于统一管理数据源
 *
 * @author zhouhao
 * @since 1.10
 */
public interface DataSourceManager {

    /**
     * 获取支持的数据源类型
     *
     * @return 数据源类型
     */
    List<DataSourceType> getSupportedType();

    /**
     * 根据类型获取数据眼供应商
     *
     * @param typeId 类型ID
     * @return 数据源供应商
     */
    DataSourceProvider getProvider(String typeId);


    /**
     * 根据类型ID获取已存在的数据源
     *
     * @param typeId 类型ID
     * @return 数据源列表
     */
    Flux<DataSource> getDataSources(String typeId);

    /**
     * 获取指定的数据源,如果数据源不存在则返回{@link Mono#empty()}
     *
     * @param type         数据源类型
     * @param datasourceId 数据源ID
     * @return 数据源
     */
    Mono<DataSource> getDataSource(DataSourceType type, String datasourceId);

    /**
     * 获取指定的数据源,如果数据源不存在则抛出异常{@link DataSourceNotExistException}
     *
     * @param type         数据源类型
     * @param datasourceId 数据源ID
     * @return 数据源
     * @see DataSourceNotExistException
     */
    default Mono<DataSource> getDataSourceOrError(DataSourceType type, String datasourceId) {
        return getDataSource(type, datasourceId)
            .switchIfEmpty(Mono.error(() -> new DataSourceNotExistException(type, datasourceId)));
    }

    /**
     * 获取指定的数据源,如果数据源不存在则返回{@link Mono#empty()}
     *
     * @param typeId       数据源类型ID
     * @param datasourceId 数据源ID
     * @return 数据源
     */
    Mono<DataSource> getDataSource(String typeId, String datasourceId);

    /**
     * 获取指定的数据源,如果数据源不存在则抛出异常{@link DataSourceNotExistException}
     *
     * @param typeId         数据源类型ID
     * @param datasourceId 数据源ID
     * @return 数据源
     * @see DataSourceNotExistException
     */
    default Mono<DataSource> getDataSourceOrError(String typeId, String datasourceId) {
        return getDataSource(typeId, datasourceId)
            .switchIfEmpty(Mono.error(() -> new DataSourceNotExistException(typeId, datasourceId)));
    }
}
