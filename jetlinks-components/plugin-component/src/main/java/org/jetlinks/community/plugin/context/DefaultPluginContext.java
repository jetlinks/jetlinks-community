/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
