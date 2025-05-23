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
package org.jetlinks.community.plugin.device;

import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.command.ProxyCommandSupport;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class PluginDeviceGateway extends AbstractDeviceGateway implements ProxyCommandSupport {

    private final DeviceGatewayPlugin plugin;

    public PluginDeviceGateway(String id, DeviceGatewayPlugin plugin) {
        super(id);
        this.plugin = plugin;
    }

    @Override
    public CommandSupport getProxyTarget() {
        return plugin;
    }

    @Override
    protected Mono<Void> doShutdown() {
        return plugin.shutdown();
    }

    @Override
    protected Mono<Void> doStartup() {
        return plugin.start();
    }

    public DeviceGatewayPlugin getPlugin() {
        return plugin;
    }

    @Nonnull
    @Override
    public <R> R execute(@Nonnull Command<R> command) {
        return plugin.execute(command);
    }

    @Override
    public <R, C extends Command<R>> C createCommand(String commandId) {
        return plugin.createCommand(commandId);
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return plugin.getCommandMetadata();
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        return plugin.getCommandMetadata(commandId);
    }


}
