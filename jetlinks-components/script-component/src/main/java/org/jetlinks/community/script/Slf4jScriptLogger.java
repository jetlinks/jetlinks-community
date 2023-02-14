package org.jetlinks.community.script;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;

@AllArgsConstructor
public class Slf4jScriptLogger implements ScriptLogger{
    private final Logger logger;

    public void trace(String text, Object... args) {
        logger.trace(text, args);
    }

    public void warn(String text, Object... args) {
        logger.warn(text, args);
    }

    public void log(String text, Object... args) {
        logger.debug(text, args);
    }

    public void error(String text, Object... args) {
        logger.error(text, args);
    }
}
