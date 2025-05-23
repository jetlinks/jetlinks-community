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
package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.notify.manager.service.NotifyHistory;
import org.jetlinks.community.notify.manager.service.NotifyHistoryRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notify/history")
@Resource(id = "notifier", name = "通知管理")
@Tag(name = "消息通知记录")
public class NotifierHistoryController {

    private final NotifyHistoryRepository repository;

    public NotifierHistoryController(NotifyHistoryRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/config/{configId}/_query")
    @Operation(summary = "根据通知配置ID查询通知记录")
    public Mono<PagerResult<NotifyHistory>> queryPagerByConfigId(@PathVariable String configId,
                                                                 @RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam
            .flatMap(param -> param
                .toNestQuery(q -> q.is(NotifyHistory::getNotifierId, configId))
                .execute(repository::queryPager));
    }

    @PostMapping("/template/{templateId}/_query")
    @Operation(summary = "根据通知模版ID查询通知记录")
    public Mono<PagerResult<NotifyHistory>> queryPagerByTemplateId(@PathVariable String templateId,
                                                                   @RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam
            .flatMap(param -> param
                .toNestQuery(q -> q.is(NotifyHistory::getTemplateId, templateId))
                .execute(repository::queryPager));
    }


}
