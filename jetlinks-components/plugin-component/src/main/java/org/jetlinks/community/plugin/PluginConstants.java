package org.jetlinks.community.plugin;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.utils.StringBuilderUtils;

public interface PluginConstants {


    /**
     * 创建用于访问插件命令的服务ID,可通过{@link org.jetlinks.community.command.CommandSupportManagerProviders#getCommandSupport(String)}来获取插件命令方法支持.
     *
     * @param pluginId 插件ID
     * @return 访问插件的服务ID
     * @see org.jetlinks.community.command.CommandSupportManagerProvider
     */
    static String createCommandServiceId(String pluginId) {
        return "plugin$" + pluginId;
    }

    interface Topics {

        SharedPathString ALL_PLUGIN_LOG = SharedPathString.of("/plugin/*/log/*");

        /**
         * <code> /plugin/{pid}/log</code>
         *
         * @param pluginId 插件ID
         * @return topic
         * @see org.jetlinks.core.event.EventBus
         * @see org.jetlinks.community.log.LogRecord
         */
        static SeparatedCharSequence pluginLog(String pluginId, String level) {
            return ALL_PLUGIN_LOG.replace(2, pluginId, 4, level);
        }

    }

}
