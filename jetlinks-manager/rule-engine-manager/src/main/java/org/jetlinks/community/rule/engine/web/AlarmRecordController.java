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
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmHandleHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/alarm/record")
@Resource(id = "alarm-record", name = "告警记录")
@Authorize
@Tag(name = "告警记录")
@AllArgsConstructor
public class AlarmRecordController implements ReactiveServiceQueryController<AlarmRecordEntity, String> {

    private final AlarmRecordService recordService;

    private final AlarmConfigService configService;

    private final AlarmHandleHistoryService handleHistoryService;

    @Override
    public ReactiveCrudService<AlarmRecordEntity, String> getService() {
        return recordService;
    }

    @PostMapping("/{dimensionType}/_query")
    @Operation(summary = "按不同维度查询告警记录")
    @QueryAction
    public Mono<PagerResult<AlarmRecordEntity>> queryPagerByDimensionType(@PathVariable String dimensionType,
                                                                          @RequestBody Mono<QueryParamEntity> query) {
        return query
            .doOnNext(queryParamEntity -> queryParamEntity
                .toNestQuery(q -> q.and(AlarmRecordEntity::getTargetType, TermType.eq, dimensionType)))
            .flatMap(this::queryPager1);
    }

    @PostMapping("/_handle")
    @Operation(summary = "处理告警")
    @SaveAction
    public Mono<Void> handleAlarm(@RequestBody Mono<AlarmHandleInfo> handleInfo) {
        return handleInfo
            .flatMap(configService::handleAlarm);
    }

    @PostMapping("/handle-history/_query")
    @Operation(summary = "告警处理历史查询")
    @QueryAction
    public Mono<PagerResult<AlarmHandleHistoryEntity>> queryHandleHistoryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(handleHistoryService::queryPager);
    }

    @PostMapping("/{id}/handle-history/_query")
    @Operation(summary = "按告警记录查询告警处理历史")
    @QueryAction
    public Mono<PagerResult<AlarmHandleHistoryEntity>> queryHandleHistoryPager(
        @PathVariable String id,
        @RequestBody Mono<QueryParamEntity> query) {
        return query
            .doOnNext(queryParamEntity -> queryParamEntity
                .toNestQuery(q -> q.and(AlarmHandleHistoryEntity::getAlarmRecordId, TermType.eq, id)))
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
        return recordService
            .createQuery()
            .where(AlarmRecordEntity::getId, recordId)
            .fetchOne()
            .switchIfEmpty(Mono.error(() -> new NotFoundException.NoStackTrace("error.alarm_record_not_exist", 500, recordId)))
            .flatMap(record -> query.flatMap(handleHistoryService::queryPager));
    }

    private Mono<PagerResult<AlarmRecordEntity>> queryPager1(QueryParamEntity query) {
        if (query.getTotal() != null) {
            return getService()
                .createQuery()
                .setParam(query.rePaging(query.getTotal()))
                .fetch()
                .collectList()
                .map(list -> PagerResult.of(query.getTotal(), list, query));
        }
        return getService().queryPager(query);
    }
}
