package org.jetlinks.community.logging.access;

import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.jetlinks.community.logging.configuration.LoggingProperties;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
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
        eventPublisher.publishEvent(SerializableAccessLog.of(event.getLogger()));
    }

}
