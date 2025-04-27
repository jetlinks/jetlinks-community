package org.jetlinks.community.logging.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.logging.system.SerializableSystemLog;
import org.jetlinks.community.logging.system.SystemLoggerService;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Slf4j
@Order(5)
public class SystemLoggerEventHandler {

    private final SystemLoggerService loggerService;

    public SystemLoggerEventHandler(SystemLoggerService loggerService) {
        this.loggerService = loggerService;
    }

    @EventListener
    public void acceptAccessLoggerInfo(SerializableSystemLog info) {
        loggerService
            .save(info)
            .subscribe();
    }


}
