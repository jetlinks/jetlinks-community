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
