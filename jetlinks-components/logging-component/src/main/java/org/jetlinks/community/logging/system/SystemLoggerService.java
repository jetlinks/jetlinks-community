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
package org.jetlinks.community.logging.system;


import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 系统日志服务,用于记录和查询系统内部运行过程中产生的日志
 *
 * @version 2.3
 * @see org.jetlinks.community.logging.logback.SystemLoggingAppender
 **/
public interface SystemLoggerService {

    /**
     * 保存系统日志
     *
     * @param log 日志内容
     * @return void
     */
    Mono<Void> save(SerializableSystemLog log);

    /**
     * 分页查询系统日志
     *
     * @param queryParam 动态查询参数
     * @return 查询结果
     */
    Mono<PagerResult<SerializableSystemLog>> query(QueryParamEntity queryParam);

    /**
     * 不分页查询系统日志
     *
     * @param queryParam 查询参数
     * @return 查询结果
     */
    Flux<SerializableSystemLog> queryNoPaging(QueryParamEntity queryParam);
}