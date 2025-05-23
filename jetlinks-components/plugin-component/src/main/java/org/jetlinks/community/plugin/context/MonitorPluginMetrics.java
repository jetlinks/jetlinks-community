package org.jetlinks.community.plugin.context;

import lombok.AllArgsConstructor;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;
import org.jetlinks.plugin.core.PluginMetrics;
@AllArgsConstructor(staticName = "of")
class MonitorPluginMetrics implements PluginMetrics {
    private final Monitor monitor;
    @Override
    public Logger logger() {
        return monitor.logger();
    }

    @Override
    public Tracer tracer() {
        return monitor.tracer();
    }

    @Override
    public Metrics metrics() {
        return monitor.metrics();
    }
}
