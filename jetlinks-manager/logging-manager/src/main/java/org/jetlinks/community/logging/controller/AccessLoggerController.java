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
package org.jetlinks.community.logging.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.logging.access.AccessLoggerService;
import org.jetlinks.community.logging.access.SerializableAccessLog;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@RestController
@RequestMapping("/logger/access")
@Resource(id = "access-logger", name = "访问日志")
@Tag(name = "日志管理")
@AllArgsConstructor
public class AccessLoggerController {

    private AccessLoggerService loggerService;

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询访问日志")
    public Mono<PagerResult<SerializableAccessLog>> getAccessLogger(@Parameter(hidden = true) QueryParamEntity queryParam) {
        return loggerService
            .query(queryParam);
    }

    @PostMapping("/_query")
    @QueryAction
    @Operation(summary = "(POST)查询访问日志")
    public Mono<PagerResult<SerializableAccessLog>> getAccessLogger(@RequestBody Mono<QueryParamEntity> queryMono) {
        return queryMono
            .flatMap(loggerService::query);
    }


}
