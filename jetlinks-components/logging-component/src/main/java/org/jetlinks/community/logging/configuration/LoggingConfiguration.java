package org.jetlinks.community.logging.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.logging.logback.SystemLoggingAppender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
@Slf4j
public class LoggingConfiguration implements ApplicationEventPublisherAware {


    private final LoggingProperties properties;

    public LoggingConfiguration(LoggingProperties properties) {
        this.properties = properties;
        SystemLoggingAppender.staticContext.putAll(properties.getSystem().getContext());
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        SystemLoggingAppender.publisher = applicationEventPublisher;
    }
}
