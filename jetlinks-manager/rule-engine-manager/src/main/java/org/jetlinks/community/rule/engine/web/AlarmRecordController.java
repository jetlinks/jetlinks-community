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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.alarm.AlarmHandler;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.service.AlarmHandleHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
@RequestMapping(value = "/alarm/record")
@Resource(id = "alarm-record", name = "告警记录")
@Authorize
@Tag(name = "告警记录")
@AllArgsConstructor
public class AlarmRecordController implements ReactiveServiceQueryController<AlarmRecordEntity, String> {

    private final AlarmRecordService alarmRecordService;

    private final AlarmHandleHistoryService handleHistoryService;

    private final AlarmHandler alarmHandler;

    @Override
    public AlarmRecordService getService() {
        return alarmRecordService;
    }


    @PostMapping("/{dimensionType}/_query")
    @Operation(summary = "按不同维度查询告警记录")
    @QueryAction
    public Mono<PagerResult<AlarmRecordEntity>> queryPagerByDimensionType(@PathVariable String dimensionType,
                                                                          @RequestBody Mono<QueryParamEntity> query) {
        return query.doOnNext(queryParamEntity -> {
                        queryParamEntity.toNestQuery(q -> q.and("targetType", TermType.eq, dimensionType));
                    })
                    .flatMap(this::queryPager1);
    }


    @PostMapping("/_handle")
    @Operation(summary = "处理告警")
    @SaveAction
    public Mono<Void> handleAlarm(@RequestBody Mono<AlarmHandleInfo> handleInfoMono) {
        return handleInfoMono
                .flatMap(info -> alarmRecordService
                        .findById(info.getAlarmRecordId())
                        .flatMap(record -> handleAlarm(record, AlarmHandleHistoryEntity.of(info)))
                );
    }

    @PostMapping("/handle-history/_query")
    @Operation(summary = "告警处理历史查询")
    @QueryAction
    @Deprecated
    public Mono<PagerResult<AlarmHandleHistoryEntity>> queryHandleHistoryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(handleHistoryService::queryPager);
    }


    @PostMapping("/{id}/handle-history/_query")
    @Operation(summary = "按告警记录查询告警处理历史")
    @QueryAction
    public Mono<PagerResult<AlarmHandleHistoryEntity>> queryHandleHistoryPager(@PathVariable String id, @RequestBody Mono<QueryParamEntity> query) {
        return query
                .doOnNext(queryParamEntity -> queryParamEntity
                        .toNestQuery(q -> q.and("alarmRecordId", TermType.eq, id)))
                .flatMap(handleHistoryService::queryPager);
    }

    @PostMapping("/{dimensionType}/_handle")
    @Operation(summary = "按维度处理告警")
    @SaveAction
    @Deprecated
    public Mono<Void> handleAlarm(@PathVariable String dimensionType,
                                  @RequestBody Mono<AlarmHandleInfo> handleInfo) {
        return handleAlarm(handleInfo);
    }

    @PostMapping("/handle-history/{dimensionType}/{recordId}/_query")
    @Operation(summary = "根据维度查询告警处理历史")
    @QueryAction
    public Mono<PagerResult<AlarmHandleHistoryEntity>> queryHandleHistoryPager(@PathVariable String dimensionType,
                                                                               @PathVariable String recordId,
                                                                               @RequestBody Mono<QueryParamEntity> query) {
        return alarmRecordService
            .createQuery()
            .where(AlarmRecordEntity::getId, recordId)
            .fetchOne()
            .switchIfEmpty(Mono.error(() -> new NotFoundException.NoStackTrace("error.alarm_record_not_exist", 500, recordId)))
            .flatMap(record -> query.flatMap(handleHistoryService::queryPager));
    }

    private Mono<Void> handleAlarm(AlarmRecordEntity record, AlarmHandleHistoryEntity entity) {
        RelieveInfo relieveInfo = FastBeanCopier.copy(record, new RelieveInfo());
        relieveInfo.setRelieveTime(entity.getHandleTime());
        relieveInfo.setRelieveReason(entity.getDescription());
        relieveInfo.setData(Collections.emptyMap());
        relieveInfo.setAlarmRelieveType(entity.getHandleType().getValue());
        relieveInfo.setDescribe(entity.getDescription());
        return alarmHandler
                .relieveAlarm(relieveInfo)
                .then();
    }

    private Mono<PagerResult<AlarmRecordEntity>> queryPager1(QueryParamEntity query) {
        if (query.getTotal() != null) {
            return this
                .query(query.rePaging(query.getTotal()))
                .collectList()
                .map(list -> PagerResult.of(query.getTotal(), list, query));
        }
        return queryPager(query);
    }
}
