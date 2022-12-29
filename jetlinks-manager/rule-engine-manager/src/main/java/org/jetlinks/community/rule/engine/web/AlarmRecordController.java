package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmHandleHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
