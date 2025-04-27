package org.jetlinks.community.logging.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.logging.access.AccessLoggerService;
import org.jetlinks.community.logging.access.AccessLoggingTranslator;
import org.jetlinks.community.logging.access.TimeSeriesAccessLoggerService;
import org.jetlinks.community.logging.event.handler.AccessLoggerEventHandler;
import org.jetlinks.community.logging.event.handler.SystemLoggerEventHandler;
import org.jetlinks.community.logging.logback.SystemLoggingAppender;
import org.jetlinks.community.logging.system.SystemLoggerService;
import org.jetlinks.community.logging.system.TimeSeriesSystemLoggerService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@Slf4j
public class LoggingConfiguration {


    @Bean
    public AccessLoggingTranslator accessLoggingTranslator(ApplicationEventPublisher eventPublisher,
                                                           LoggingProperties properties) {
        SystemLoggingAppender.publisher = eventPublisher;
        SystemLoggingAppender.staticContext.putAll(properties.getSystem().getContext());

        return new AccessLoggingTranslator(eventPublisher, properties);
    }

    @Bean
    @ConditionalOnMissingBean(AccessLoggerService.class)
    public TimeSeriesAccessLoggerService accessLoggerService(TimeSeriesManager timeSeriesManager) {
        return new TimeSeriesAccessLoggerService(timeSeriesManager);
    }

    @Bean
    @ConditionalOnMissingBean(SystemLoggerService.class)
    public TimeSeriesSystemLoggerService systemLoggerService(TimeSeriesManager timeSeriesManager) {
        return new TimeSeriesSystemLoggerService(timeSeriesManager);
    }


    @Bean
    public AccessLoggerEventHandler accessLoggerEventHandler(AccessLoggerService loggerService) {
        return new AccessLoggerEventHandler(loggerService);
    }


    @Bean
    public SystemLoggerEventHandler systemLoggerEventHandler(SystemLoggerService loggerService) {
        return new SystemLoggerEventHandler(loggerService);
    }

}
