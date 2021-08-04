package org.jetlinks.community.logging.configuration;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.logging.system.SerializableSystemLog;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "jetlinks.logging")
@Getter
@Setter
public class LoggingProperties {

    /**
     * 系统日志
     *
     * @see lombok.extern.slf4j.Slf4j
     * @see org.slf4j.Logger
     * @see org.jetlinks.community.logging.logback.SystemLoggingAppender
     * @see SerializableSystemLog
     * @see org.jetlinks.community.logging.event.SystemLoggingEvent
     */
    @Getter
    @Setter
    private SystemLoggingProperties system = new SystemLoggingProperties();

    /**
     * 访问日志
     *
     * @see org.hswebframework.web.logging.AccessLogger
     * @see org.hswebframework.web.loggin.aop.EnableAccessLogger
     * @see org.jetlinks.community.logging.event.AccessLoggingEvent
     * @see org.jetlinks.community.logging.access.SerializableAccessLog
     */
    @Setter
    @Getter
    private AccessLoggingProperties access = new AccessLoggingProperties();

    @Getter
    @Setter
    public static class SystemLoggingProperties {
        /**
         * 系统日志上下文,通常用于在日志中标识当前服务等
         *
         * @see org.hswebframework.web.logger.ReactiveLogger#mdc(String, String)
         * @see org.slf4j.MDC
         */
        private Map<String, String> context = new HashMap<>();

    }

    @Getter
    @Setter
    public static class AccessLoggingProperties {
        //指定按path过滤日志
        private List<String> pathExcludes = new ArrayList<>();
    }

}
