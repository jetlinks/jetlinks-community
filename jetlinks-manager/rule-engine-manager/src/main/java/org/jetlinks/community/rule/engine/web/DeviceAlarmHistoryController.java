package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.jetlinks.community.rule.engine.service.DeviceAlarmHistoryService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;

@RestController
@RequestMapping("/device/alarm/history")
@Resource(id = "device-alarm", name = "设备告警")
@Authorize
@Tag(name = "设备告警记录")
public class DeviceAlarmHistoryController implements ReactiveServiceQueryController<DeviceAlarmHistoryEntity, String> {

    private final DeviceAlarmHistoryService historyService;


    public DeviceAlarmHistoryController(DeviceAlarmHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public DeviceAlarmHistoryService getService() {
        return historyService;
    }

    @PutMapping("/{id}/_{state}")
    @Operation(summary = "修改告警记录状态")
    public Mono<Integer> changeState(@PathVariable @Parameter(description = "告警记录ID") String id,
                                     @PathVariable @Parameter(description = "状态") String state,
                                     @RequestBody @Parameter(description = "备注") Mono<String> descriptionMono) {
        return descriptionMono
            .flatMap(description -> historyService
                .createUpdate()
                .set(DeviceAlarmHistoryEntity::getUpdateTime, new Date())
                .set(DeviceAlarmHistoryEntity::getState, state)
                .set(DeviceAlarmHistoryEntity::getDescription, description)
                .where(DeviceAlarmHistoryEntity::getId, id)
                .execute());
    }

}
