package org.jetlinks.community.logging.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.logging.access.AccessLoggerService;
import org.jetlinks.community.logging.access.SerializableAccessLog;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
@Order(5)
public class AccessLoggerEventHandler {

    private final AccessLoggerService loggerService;

    public AccessLoggerEventHandler(AccessLoggerService loggerService) {
        this.loggerService = loggerService;
    }


    @EventListener
    public void acceptAccessLoggerInfo(SerializableAccessLog info) {
        loggerService.save(info).subscribe();
    }


}
