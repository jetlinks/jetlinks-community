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

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.community.plugin.utils.PluginUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.ToDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.util.function.Function;

public class PluginDeviceSession implements PersistentSession {
    private final DeviceOperator device;
    @Setter
    @Getter
    private DeviceGatewayPlugin plugin;

    @Getter
    @Setter
    private PluginDataIdMapper idMapper;

    private long connectTime = System.currentTimeMillis();

    private long pingTime = connectTime;

    private long timeout = -1;

    public PluginDeviceSession(DeviceOperator device) {
        this.device = device;
    }

    @Override
    public String getId() {
        return device.getId();
    }

    @Override
    public String getDeviceId() {
        return device.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return device;
    }

    @Override
    public long lastPingTime() {
        return pingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Reactors.ALWAYS_FALSE;
    }

    @Override
    public Transport getTransport() {
        return PluginTransport.plugin;
    }

    @Override
    public void close() {

    }

    @Override
    public void keepAlive() {
        pingTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        this.timeout = timeout.toMillis();
    }

    @Override
    public void ping() {
        pingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return timeout <= 0 || System.currentTimeMillis() - pingTime < timeout;
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        //检查真实状态?
        return PersistentSession.super.isAliveAsync();
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public Mono<Boolean> send(ToDeviceMessageContext context) {
        DeviceGatewayPlugin plugin = this.plugin;
        PluginDataIdMapper idMapper = this.idMapper;
        if (plugin == null || idMapper == null) {
            return Mono.error(
                new DeviceOperationException
                    .NoStackTrace(ErrorCode.SERVER_NOT_AVAILABLE, "error.plugin_not_found")
            );
        }
        return context
            .reply(
                PluginUtils
                    .transformToExternalMessage(idMapper, plugin, ((DeviceMessage) context.getMessage()).copy())
                    .flatMapMany(plugin::execute)
                    .flatMap(reply -> PluginUtils.transformToInternalMessage(idMapper, plugin, reply.copy()))
            )
            .then(Reactors.ALWAYS_TRUE);
    }

    @Override
    public String getProvider() {
        return PluginDeviceGatewayProvider.PROVIDER;
    }

    @SneakyThrows
    void write(ObjectOutput out) {

        out.writeUTF(getDeviceId());
        out.writeLong(connectTime);
        out.writeLong(pingTime);
        out.writeLong(timeout);
        SerializeUtils.writeNullableUTF(plugin == null ? null : plugin.getId(), out);
    }

    @SneakyThrows
    @SuppressWarnings("all")
    static Mono<PluginDeviceSession> read(ObjectInput input,
                                          DeviceRegistry registry,
                                          PluginDataIdMapper idMapper,
                                          Function<String, DeviceGatewayPlugin> pluginLoader) {
        String deviceId = input.readUTF();
        long connectTime = input.readLong();
        long pingTime = input.readLong();
        long timeout = input.readLong();
        String pluginId = SerializeUtils.readNullableUTF(input);

        return registry
            .getDevice(deviceId)
            .map(device -> {
                PluginDeviceSession session = new PluginDeviceSession(device);
                session.connectTime = connectTime;
                session.pingTime = pingTime;
                session.idMapper = idMapper;
                session.timeout = timeout;
                if (pluginId != null) {
                    session.plugin = pluginLoader.apply(pluginId);
                }
                return session;
            });
    }
}
