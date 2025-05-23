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
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.logging.system.SerializableSystemLog;
import org.jetlinks.community.logging.system.SystemLoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@RestController
@RequestMapping("/logger/system")
@Resource(id="system-logger",name = "系统日志")
@Tag(name = "日志管理")
public class SystemLoggerController {

    @Autowired
    private SystemLoggerService loggerService;

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询系统日志")
    public Mono<PagerResult<SerializableSystemLog>> getSystemLogger(@Parameter(hidden = true) QueryParamEntity queryParam) {
        return loggerService.query(queryParam);
    }

    @PostMapping("/_query")
    @QueryAction
    @Operation(summary = "(POST)查询系统日志")
    public Mono<PagerResult<SerializableSystemLog>> getSystemLogger(@RequestBody Mono<QueryParamEntity> queryMono) {
        return queryMono
            .flatMap(queryParam -> loggerService.query(queryParam));
    }


}
