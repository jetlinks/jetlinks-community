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
