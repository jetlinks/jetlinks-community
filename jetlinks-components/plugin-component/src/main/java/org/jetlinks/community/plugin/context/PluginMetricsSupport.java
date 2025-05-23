package org.jetlinks.community.plugin.context;

import org.jetlinks.plugin.core.PluginMetrics;

@Deprecated
public interface PluginMetricsSupport {

    static PluginMetrics create(String pluginId){
        return NonePluginMetrics.INSTANCE;
    }
}
