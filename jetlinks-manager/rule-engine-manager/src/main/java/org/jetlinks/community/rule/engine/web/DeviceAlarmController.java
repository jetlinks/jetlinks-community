package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.service.DeviceAlarmService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/device/alarm")
@Resource(id = "device-alarm", name = "设备告警")
@Authorize
@Tag(name = "设备告警配置")
public class DeviceAlarmController implements ReactiveServiceQueryController<DeviceAlarmEntity, String> {

    private final DeviceAlarmService alarmService;

    public DeviceAlarmController(DeviceAlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @Override
    public ReactiveCrudService<DeviceAlarmEntity, String> getService() {
        return alarmService;
    }

    @GetMapping("/{target}/{targetId}")
    @QueryAction
    @Operation(summary = "获取告警配置")
    public Flux<DeviceAlarmEntity> getAlarms(@PathVariable @Parameter(description = "device或者product") String target,
                                             @PathVariable @Parameter(description = "deviceId或者productId") String targetId) {
        return alarmService.createQuery()
            .where(DeviceAlarmEntity::getTarget, target)
            .and(DeviceAlarmEntity::getTargetId, targetId)
            .fetch();
    }

    @PatchMapping("/{target}/{targetId}")
    @QueryAction
    @Operation(summary = "保存告警配置")
    public Mono<Void> saveAlarm(@PathVariable @Parameter(description = "device或者product") String target,
                                @PathVariable @Parameter(description = "deviceId或者productId") String targetId,
                                @RequestBody Mono<DeviceAlarmEntity> payload) {
        return payload
            .doOnNext(dev -> {
                dev.setTarget(target);
                dev.setTargetId(targetId);
            })
            .as(alarmService::save)
            .then();
    }

    @PostMapping("/{id}/_start")
    @SaveAction
    @Operation(summary = "启动告警配置")
    public Mono<Void> startAlarm(@PathVariable String id) {

        return alarmService.start(id);
    }

    @PostMapping("/{id}/_stop")
    @SaveAction
    @Operation(summary = "停止告警配置")
    public Mono<Void> stopAlarm(@PathVariable String id) {
        return alarmService.stop(id);
    }

    @DeleteMapping("/{id}")
    @SaveAction
    @Operation(summary = "删除告警配置")
    public Mono<Void> deleteAlarm(@PathVariable String id) {
        return alarmService.deleteById(Mono.just(id))
            .then();
    }

}
