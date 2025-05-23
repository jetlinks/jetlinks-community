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
package org.jetlinks.community.network.mqtt.gateway.device.session;

import lombok.SneakyThrows;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionProvider;
import org.jetlinks.core.server.session.DeviceSessionProviders;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;

import static org.jetlinks.community.codec.Serializers.getDefault;

@Component
public class MqttClientSessionPersistentProvider implements DeviceSessionProvider {
    public static final String PROVIDER = "mqtt-client";

    private final NetworkManager networkManager;

    public MqttClientSessionPersistentProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
        DeviceSessionProviders.register(this);
    }

    @Override
    public String getId() {
        return PROVIDER;
    }

    @Override
    public Mono<PersistentSession> deserialize(byte[] sessionData, DeviceRegistry registry) {

        return Mono
            .fromCallable(() -> {
                ByteArrayInputStream stream = new ByteArrayInputStream(sessionData);
                SessionData data = new SessionData();
                try (ObjectInput input = getDefault().createInput(stream)) {
                    data.readExternal(input);
                }
                return data;
            })
            .flatMap(data -> data.toSession(registry, networkManager));
    }

    @Override
    public Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry) {
        if (!session.isWrapFrom(MqttClientSession.class)) {
            return Mono.empty();
        }
        return SessionData
            .of(session.unwrap(MqttClientSession.class))
            .flatMap(data -> Mono
                .fromCallable(() -> {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream(128);
                    try (ObjectOutput output = getDefault().createOutput(stream)) {
                        data.writeExternal(output);
                    }
                    return stream.toByteArray();
                }));
    }

    static class SessionData {
        private String deviceId;
        private String networkId;
        private String gatewayId;
        private long lastPingTime;
        private long connectTime;
        private long keepAliveTimeout;

        public SessionData() {
        }

        public static Mono<SessionData> of(MqttClientSession session) {
            SessionData data = new SessionData();
            data.deviceId = session.getDeviceId();
            data.gatewayId = session.getGatewayId();
            data.lastPingTime = session.lastPingTime();
            data.connectTime = session.connectTime();
            data.keepAliveTimeout = session.getKeepAliveTimeout().toMillis();
            return session
                .getClient()
                .doOnNext(client -> data.networkId = client.getId())
                .thenReturn(data);
        }

        public Mono<PersistentSession> toSession(DeviceRegistry registry,
                                                 NetworkManager manager) {
            return registry
                .getDevice(deviceId)
                .mapNotNull(device -> {
                    if (networkId == null || gatewayId == null) {
                        return null;
                    }
                    MqttClientSession session = new MqttClientSession(
                        deviceId,
                        device,
                        manager.getNetwork(DefaultNetworkType.MQTT_CLIENT, networkId),
                        GatewayMonitors.getDeviceGatewayMonitor(gatewayId));

                    session.setKeepAliveTimeout(Duration.ofMillis(keepAliveTimeout));
                    session.setLastPingTime(lastPingTime);
                    session.setConnectTime(connectTime);
                    session.setGatewayId(gatewayId);
                    return session;
                });
        }

        @SneakyThrows
        public void writeExternal(ObjectOutput out) {
            out.writeUTF(deviceId);
            out.writeUTF(networkId);
            SerializeUtils.writeNullableUTF(gatewayId, out);
            out.writeLong(lastPingTime);
            out.writeLong(connectTime);
            out.writeLong(keepAliveTimeout);
        }

        @SneakyThrows
        public void readExternal(ObjectInput in) {
            deviceId = in.readUTF();
            networkId = in.readUTF();
            gatewayId = SerializeUtils.readNullableUTF(in);
            lastPingTime = in.readLong();
            connectTime = in.readLong();
            keepAliveTimeout = in.readLong();
        }
    }
}
