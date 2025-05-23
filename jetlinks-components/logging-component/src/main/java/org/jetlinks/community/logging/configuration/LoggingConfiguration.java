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
