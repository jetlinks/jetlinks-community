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
