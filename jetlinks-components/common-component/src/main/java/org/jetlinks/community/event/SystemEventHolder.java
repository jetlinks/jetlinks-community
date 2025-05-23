/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
