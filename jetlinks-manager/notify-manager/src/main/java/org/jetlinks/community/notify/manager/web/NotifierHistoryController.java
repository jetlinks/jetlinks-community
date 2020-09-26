package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.notify.manager.entity.NotifyHistoryEntity;
import org.jetlinks.community.notify.manager.service.NotifyHistoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notify/history")
@Tag(name = "消息通知记录")
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
