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
    public Mono<PagerResult<AlarmHistoryInfo>> queryHandleHistoryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(alarmHistoryService::queryPager);
    }

    @PostMapping("/{alarmConfigId}/_query")
    @Operation(summary = "告警历史查询")
    @QueryAction
    public Mono<PagerResult<AlarmHistoryInfo>> queryHandleHistoryPager(
        @PathVariable @Parameter(description = "告警配置ID") String alarmConfigId,
        @RequestBody Mono<QueryParamEntity> query
    ) {
        return query
            .map(q -> q
                .toNestQuery()
                .and(AlarmHistoryInfo::getAlarmConfigId, alarmConfigId)
                .getParam())
            .flatMap(alarmHistoryService::queryPager);
    }

}
