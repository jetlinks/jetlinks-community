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
package org.jetlinks.community.protocol;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.spi.ServiceContext;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "of")
public class CompositeServiceContext implements ServiceContext {
    private final List<ServiceContext> contexts;

    private final Monitor monitor;
    private final Function<String, Monitor> deviceMonitor;

    public static ServiceContext of(Monitor monitor,
                                    Function<String, Monitor> deviceMonitor,
                                    ServiceContext... context) {
        return new CompositeServiceContext(Arrays.asList(context), monitor, deviceMonitor);
    }

    public static ServiceContext of(ServiceContext... context) {
        return new CompositeServiceContext(Arrays.asList(context), Monitor.noop(), ignore -> Monitor.noop());
    }

    public static ServiceContext of(List<ServiceContext> context) {
        return new CompositeServiceContext(context, Monitor.noop(), ignore -> Monitor.noop());
    }


    @Override
    public Optional<Value> getConfig(ConfigKey<String> key) {
        for (ServiceContext context : contexts) {
            Optional<Value> value = context.getConfig(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Value> getConfig(String key) {
        for (ServiceContext context : contexts) {
            Optional<Value> value = context.getConfig(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(Class<T> service) {
        for (ServiceContext context : contexts) {
            Optional<T> value = context.getService(service);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(String service, Class<T> type) {
        for (ServiceContext context : contexts) {
            Optional<T> value = context.getService(service, type);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getService(String service) {
        for (ServiceContext context : contexts) {
            Optional<T> value = context.getService(service);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getServices(Class<T> service) {

        return contexts
            .stream()
            .flatMap(ctx -> ctx.getServices(service).stream())
            .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> getServices(String service) {
        return contexts
            .stream()
            .flatMap(ctx -> ctx.<T>getServices(service).stream())
            .collect(Collectors.toList());
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public Monitor getMonitor(String deviceId) {
        return deviceMonitor.apply(deviceId);
    }
}
