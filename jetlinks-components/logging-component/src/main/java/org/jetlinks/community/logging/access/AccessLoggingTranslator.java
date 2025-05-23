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
package org.jetlinks.community.logging.access;

import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.community.logging.configuration.LoggingProperties;
import org.jetlinks.community.logging.utils.LoggingUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Mono;

public class AccessLoggingTranslator {

    private final ApplicationEventPublisher eventPublisher;

    private final LoggingProperties properties;

    public AccessLoggingTranslator(ApplicationEventPublisher eventPublisher, LoggingProperties properties) {
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @EventListener
    public void translate(AccessLoggerAfterEvent event) {

        for (String pathExclude : properties.getAccess().getPathExcludes()) {
            if (TopicUtils.match(pathExclude, event.getLogger().getUrl())) {
                return;
            }
        }
        SerializableAccessLog log = SerializableAccessLog.of(event.getLogger());

        eventPublisher.publishEvent(log);
    }


}
