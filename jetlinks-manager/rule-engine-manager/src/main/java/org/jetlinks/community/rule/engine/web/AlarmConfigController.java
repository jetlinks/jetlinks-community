package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.alarm.AlarmLevelInfo;
import org.jetlinks.community.rule.engine.alarm.AlarmTargetSupplier;
import org.jetlinks.community.rule.engine.entity.AlarmConfigDetail;
import org.jetlinks.community.rule.engine.entity.AlarmConfigEntity;
import org.jetlinks.community.rule.engine.entity.AlarmLevelEntity;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmLevelService;
import org.jetlinks.community.rule.engine.web.response.AlarmTargetTypeInfo;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/alarm/config")
@Resource(id = "alarm-config", name = "告警配置")
@Authorize
@Tag(name = "告警配置")
@AllArgsConstructor
public class AlarmConfigController implements ReactiveServiceCrudController<AlarmConfigEntity, String> {
    private final AlarmConfigService alarmConfigService;

    private final ReactiveRepository<AlarmLevelEntity, String> alarmLevelRepository;

    @Override
    public ReactiveCrudService<AlarmConfigEntity, String> getService() {
        return alarmConfigService;
    }

    @PostMapping("/{id}/_enable")
    @Operation(summary = "启用告警配置")
    public Mono<Void> enable(@PathVariable String id) {
        return alarmConfigService.enable(id);
    }

    @PostMapping("/{id}/_disable")
    @Operation(summary = "禁用告警配置")
    public Mono<Void> disable(@PathVariable String id) {
        return alarmConfigService.disable(id);
    }

    @GetMapping("/target-type/supports")
    @Operation(summary = "获取支持的告警目标类型")
    public Flux<AlarmTargetTypeInfo> getTargetTypeSupports() {
        return Flux
            .fromIterable(AlarmTargetSupplier
                              .get()
                              .getAll()
                              .values())
            .map(AlarmTargetTypeInfo::of);
    }


    @PatchMapping("/default/level")
    @Operation(summary = "保存默认告警级别")
    @SaveAction
    public Mono<Void> saveAlarmLevel(@RequestBody Flux<AlarmLevelInfo> levelInfo) {
        return levelInfo
            .collectList()
            .flatMap(info -> alarmLevelRepository.save(AlarmLevelEntity.defaultOf(info)))
            .then();
    }

    @PatchMapping("/level")
    @Operation(summary = "保存告警级别")
    @SaveAction
    public Mono<Void> saveAlarmLevel(@RequestBody Mono<AlarmLevelEntity> entity) {
        return alarmLevelRepository
            .save(entity)
            .then();
    }

    @PostMapping("/detail/_query")
    @Operation(summary = "查询告警配置详情")
    @QueryAction
    public Mono<PagerResult<AlarmConfigDetail>> queryDetailPager(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(alarmConfigService::queryDetailPager);
    }

    @GetMapping("/default/level")
    @Operation(summary = " 获取默认告警级别")
    @QueryAction
    public Mono<AlarmLevelEntity> queryAlarmLevel() {
        return alarmLevelRepository.findById(AlarmLevelService.DEFAULT_ALARM_ID);
    }
}
