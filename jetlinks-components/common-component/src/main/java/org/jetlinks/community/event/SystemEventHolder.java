package org.jetlinks.community.event;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.Operation;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class SystemEventHolder {

    private static final List<SystemEventHandler> eventHandlers = new CopyOnWriteArrayList<>();


    public static Disposable register(SystemEventHandler handler) {
        eventHandlers.add(handler);
        return () -> eventHandlers.remove(handler);
    }

    public static void error(Operation operation, String code, Object detail) {
        log.error("{} {} :{}", operation, code, detail);
        if (eventHandlers.isEmpty()) {
            return;
        }
        fireEvent(new SystemEvent(SystemEvent.Level.error, code, operation, detail));
    }

    public static void warn(Operation operation, String code, Object detail) {
        log.warn("{} {} :{}", operation, code, detail);
        if (eventHandlers.isEmpty()) {
            return;
        }
        fireEvent(new SystemEvent(SystemEvent.Level.warn, code, operation, detail));
    }

    public static void info(Operation operation, String code, Object detail) {
        log.info("{} {} :{}", operation, code, detail);
        if (eventHandlers.isEmpty()) {
            return;
        }
        fireEvent(new SystemEvent(SystemEvent.Level.info, code, operation, detail));
    }

    private static void fireEvent(SystemEvent event) {
        for (SystemEventHandler eventHandler : eventHandlers) {
            try {
                eventHandler.handle(event);
            } catch (Throwable e) {
                log.warn("handle system log error", e);
            }
        }
    }
}
