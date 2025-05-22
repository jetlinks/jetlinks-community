package org.jetlinks.community.plugin.context;

import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.plugin.core.*;

import java.io.File;

public class DefaultPluginContext implements PluginContext {
    private final PluginRegistry pluginRegistry;
    private final ServiceRegistry serviceRegistry;
    private final PluginEnvironment environment;
    private final PluginMetrics metrics;
    private final PluginScheduler scheduler;
    private final File workDir;

    private DefaultPluginContext(PluginRegistry pluginRegistry,
                                 ServiceRegistry serviceRegistry,
                                 PluginEnvironment environment,
                                 PluginMetrics metrics,
                                 PluginScheduler scheduler,
                                 File workDir) {
        this.pluginRegistry = pluginRegistry;
        this.serviceRegistry = serviceRegistry;
        this.environment = environment;
        this.metrics = metrics;
        this.scheduler = scheduler;
        this.workDir = workDir;
    }

    @Deprecated
    public static DefaultPluginContext of(PluginRegistry pluginRegistry,
                                          ServiceRegistry serviceRegistry,
                                          PluginEnvironment environment,
                                          PluginMetrics metrics,
                                          PluginScheduler scheduler,
                                          File workDir) {
        return new DefaultPluginContext(pluginRegistry, serviceRegistry, environment, metrics, scheduler, workDir);
    }

    public static DefaultPluginContext of(PluginRegistry pluginRegistry,
                                          ServiceRegistry serviceRegistry,
                                          PluginEnvironment environment,
                                          Monitor monitor,
                                          PluginScheduler scheduler,
                                          File workDir) {
        return new DefaultPluginContext(pluginRegistry,
                                        serviceRegistry,
                                        environment,
                                        MonitorPluginMetrics.of(monitor),
                                        scheduler,
                                        workDir);
    }

    @Override
    public PluginRegistry registry() {
        return pluginRegistry;
    }

    @Override
    public ServiceRegistry services() {
        return serviceRegistry;
    }

    @Override
    public PluginEnvironment environment() {
        return environment;
    }

    @Override
    public PluginMetrics metrics() {
        return metrics;
    }

    @Override
    public Monitor monitor() {
        return metrics;
    }

    @Override
    public PluginScheduler scheduler() {
        return scheduler;
    }

    @Override
    public File workDir() {
        return workDir;
    }
}
