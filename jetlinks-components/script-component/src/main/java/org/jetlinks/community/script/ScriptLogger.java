package org.jetlinks.community.script;

public interface ScriptLogger {
    default void trace(String text, Object... args) {
    }

    default void warn(String text, Object... args) {
    }

    default void log(String text, Object... args) {
    }

    default void error(String text, Object... args) {
    }
}
