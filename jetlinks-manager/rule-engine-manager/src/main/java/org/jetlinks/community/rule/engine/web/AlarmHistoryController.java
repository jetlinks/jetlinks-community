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
package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/alarm/history")
@Resource(id = "alarm-record", name = "告警记录")
@Authorize
@Tag(name = "告警历史")
@AllArgsConstructor
public class AlarmHistoryController {

    private final AlarmHistoryService alarmHistoryService;

    @PostMapping("/_query")
    @Operation(summary = "告警历史查询")
    @QueryAction
    @Deprecated
    // 已弃用，改为通过告警记录的数据权限查询
    public Mono<PagerResult<AlarmHistoryInfo>> queryHandleHistoryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(alarmHistoryService::queryPager);
    }

    @PostMapping("/{alarmConfigId}/_query")
    @Operation(summary = "按告警配置查询告警历史")
    @QueryAction
    @Deprecated
    // 已弃用，改为通过告警记录的数据权限查询
    public Mono<PagerResult<AlarmHistoryInfo>> queryHandleHistoryPager(
            @PathVariable @Parameter(description = "告警配置ID") String alarmConfigId,
            @RequestBody Mono<QueryParamEntity> query) {
        return query
                .map(q -> q
                        .toNestQuery()
                        .and(AlarmHistoryInfo::getAlarmConfigId, alarmConfigId)
                        .getParam())
                .flatMap(alarmHistoryService::queryPager);
    }

    @PostMapping("/alarm-record/{recordId}/_query")
    @Operation(summary = "按告警记录查询告警历史")
    @QueryAction
    public Mono<PagerResult<AlarmHistoryInfo>> queryHistoryPager(
        @PathVariable @Parameter(description = "告警记录ID") String recordId,
        @RequestBody Mono<QueryParamEntity> query) {
        return query
            .map(q -> q
                .toNestQuery()
                .and(AlarmHistoryInfo::getAlarmRecordId, recordId)
                .getParam())
            .flatMap(alarmHistoryService::queryPager);
    }

    @PostMapping("/{dimensionType}/{alarmConfigId}/_query")
    @Operation(summary = "按维度查询告警历史")
    @QueryAction
    public Mono<PagerResult<AlarmHistoryInfo>> queryHandleHistoryPagerByDimensionType(@PathVariable @Parameter(description = "告警配置ID") String alarmConfigId,
                                                                                      @PathVariable @Parameter(description = "告警维度") String dimensionType,
                                                                                      @RequestBody Mono<QueryParamEntity> query) {
        return query
            .doOnNext(queryParamEntity -> queryParamEntity
                .toNestQuery(q -> q.and(AlarmHistoryInfo::getAlarmConfigId, alarmConfigId)))
            .flatMap(alarmHistoryService::queryPager);
    }

}
