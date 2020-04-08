package org.jetlinks.community.rule.engine.web;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.service.DeviceAlarmService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/device/alarm")
@Resource(id = "device-alarm", name = "设备告警")
@Authorize
public class DeviceAlarmController implements ReactiveServiceCrudController<DeviceAlarmEntity, String> {

    private final DeviceAlarmService alarmService;

    public DeviceAlarmController(DeviceAlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @Override
    public DeviceAlarmService getService() {
        return alarmService;
    }

    @GetMapping("/{target}/{targetId}")
    @QueryAction
    public Flux<DeviceAlarmEntity> getProductAlarms(@PathVariable String target,
                                                    @PathVariable String targetId) {
        return alarmService.createQuery()
            .where(DeviceAlarmEntity::getTarget, target)
            .and(DeviceAlarmEntity::getTargetId, targetId)
            .fetch();
    }

    @PatchMapping("/{target}/{targetId}")
    @QueryAction
    public Mono<Void> saveAlarm(@PathVariable String target,
                                @PathVariable String targetId,
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
    public Mono<Void> startAlarm(@PathVariable String id) {
        return alarmService.start(id);
    }

    @PostMapping("/{id}/_stop")
    @SaveAction
    public Mono<Void> stopAlarm(@PathVariable String id) {
        return alarmService.stop(id);
    }

}
