package org.jetlinks.community.plugin.monitor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.community.monitor.AbstractEventMonitor;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class PluginMonitorHelper {
    private static final SharedPathString ALL_PLUGIN_LOGGER =
        SharedPathString.of("/_monitor/plugin/*/*/logger");

    static final SharedPathString tracePrefix = SharedPathString.of("/plugin/*/*");


    static SeparatedCharSequence createLoggerTopicPrefix(String type, String pluginId) {
        return ALL_PLUGIN_LOGGER.replace(3, type, 4, pluginId);
    }

    public static SeparatedCharSequence createLoggerTopic(String type, String pluginId, String level) {
        return createLoggerTopicPrefix(type, pluginId).append(level);
    }

    /**
     * 创建插件监控
     * <p>
     * 可通过{@link PluginMonitorHelper#createLoggerTopic(String, String, String)}来订阅日志.
     *
     * @param eventBus 事件总线
     * @param type     插件类型
     * @param pluginId 插件ID
     * @return 插件监控
     */
    public static Monitor createMonitor(EventBus eventBus, String type, String pluginId) {
        return new PluginMonitor(eventBus, type, pluginId);
    }


    @Getter
    private final static class PluginMonitor extends AbstractEventMonitor {
        private final org.slf4j.Logger logger;

        private PluginMonitor(EventBus eventBus, String type, String id) {
            super(eventBus,
                  tracePrefix.replace(2, type, 3, id),
                  createLoggerTopicPrefix(type, id)
            );
            logger = LoggerFactory.getLogger("org.jetlinks.plugin.monitor." + type);
        }

        @Override
        protected CharSequence getLogType() {
            return this.loggerEventPrefix.range(4, this.loggerEventPrefix.size() - 1);
        }
    }
}
