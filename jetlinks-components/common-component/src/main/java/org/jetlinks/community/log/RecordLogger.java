package org.jetlinks.community.log;

import org.jetlinks.core.monitor.logger.Logger;
import org.slf4j.event.Level;

import java.util.function.Supplier;

public abstract class RecordLogger implements Logger {

    @Override
    public final void log(Level level, String message, Object... args) {

        handleLog(() -> new LogRecord().withLog(level, message, args));
    }


    protected abstract void handleLog(Supplier<LogRecord> supplier);

}
