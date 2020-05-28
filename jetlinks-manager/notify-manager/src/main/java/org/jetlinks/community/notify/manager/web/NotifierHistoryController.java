package org.jetlinks.community.notify.manager.web;

import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.notify.manager.entity.NotifyHistoryEntity;
import org.jetlinks.community.notify.manager.service.NotifyHistoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notify/history")
@Resource(id = "notifier", name = "通知管理")
public class NotifierHistoryController implements ReactiveServiceQueryController<NotifyHistoryEntity, String> {

    private final NotifyHistoryService historyService;

    public NotifierHistoryController(NotifyHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public NotifyHistoryService getService() {
        return historyService;
    }
}
