package org.jetlinks.community.rule.engine.web;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.jetlinks.community.rule.engine.service.DeviceAlarmHistoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device/alarm/history")
@Resource(id = "device-alarm", name = "设备告警")
@Authorize
public class DeviceAlarmHistoryController implements ReactiveServiceQueryController<DeviceAlarmHistoryEntity, String> {

    private final DeviceAlarmHistoryService historyService;


    public DeviceAlarmHistoryController(DeviceAlarmHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public DeviceAlarmHistoryService getService() {
        return historyService;
    }


}
