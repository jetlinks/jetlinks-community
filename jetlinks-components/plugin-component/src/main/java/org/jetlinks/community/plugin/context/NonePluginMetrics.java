package org.jetlinks.community.plugin.context;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;
import org.jetlinks.plugin.core.PluginMetrics;

public class NonePluginMetrics implements PluginMetrics {
    public static final NonePluginMetrics INSTANCE = new NonePluginMetrics();

    private NonePluginMetrics() {

    }

    @Override
    public Logger logger() {
        return Logger.noop();
    }

    @Override
    public Tracer tracer() {
        return Tracer.noop();
    }

    @Override
    public Metrics metrics() {
        return Metrics.noop();
    }
}
