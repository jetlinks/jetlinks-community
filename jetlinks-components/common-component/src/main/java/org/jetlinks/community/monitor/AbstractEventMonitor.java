package org.jetlinks.community.monitor;

import org.jetlinks.community.log.LogRecord;
import org.jetlinks.core.Lazy;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.logger.Slf4jLoggerAdapter;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.SimpleTracer;
import org.jetlinks.core.monitor.tracer.Tracer;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import java.util.function.Supplier;

public abstract class AbstractEventMonitor extends SimpleTracer implements Monitor, Slf4jLoggerAdapter {

    protected final EventBus eventBus;
    protected final SeparatedCharSequence loggerEventPrefix;

    public AbstractEventMonitor(EventBus eventBus,
                                SeparatedCharSequence tracePrefix,
                                SeparatedCharSequence loggerPrefix) {
        super(tracePrefix);
        this.loggerEventPrefix = loggerPrefix;
        this.eventBus = eventBus;
    }

    protected abstract CharSequence getLogType();

    @Override
    public boolean isEnabled(Level level) {
        return true;
    }

    @Override
    public final void log(Level level, String message, Object... args) {
        Supplier<LogRecord> supplier = Lazy.of(
            () -> new LogRecord().withLog(level, message, args)
        );

        if (Slf4jLoggerAdapter.super.isEnabled(level)) {
            LogRecord record = supplier.get();
            Throwable error = MessageFormatter.getThrowableCandidate(args);
            if (error != null) {
                Slf4jLoggerAdapter.super.log(level, "[{}] - {}", getLogType(), record.getMessage(), error);
            } else {
                Slf4jLoggerAdapter.super.log(level, "[{}] - {}", getLogType(), record.getMessage());
            }
        }
        //推送到事件总线
        eventBus
            .publish(loggerEventPrefix.append(level.name()), supplier)
            .subscribe();

    }

    @Override
    public abstract org.slf4j.Logger getLogger();

    @Override
    public final Logger logger() {
        return this;
    }

    @Override
    public final Tracer tracer() {
        return this;
    }

    @Override
    public Metrics metrics() {
        return Metrics.noop();
    }
}
