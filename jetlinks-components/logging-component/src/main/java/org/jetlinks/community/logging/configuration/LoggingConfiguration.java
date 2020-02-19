package org.jetlinks.community.logging.configuration;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.CreateIndex;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.jetlinks.community.logging.event.handler.LoggerIndexProvider;
import org.jetlinks.community.logging.logback.SystemLoggingAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;

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
