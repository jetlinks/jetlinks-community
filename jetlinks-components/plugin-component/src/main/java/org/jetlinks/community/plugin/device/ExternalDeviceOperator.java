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

import lombok.AllArgsConstructor;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.*;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
public class ExternalDeviceOperator implements DeviceOperator {

    private final String externalId;
    private final String pluginId;
    private final PluginDataIdMapper idMapper;
    private final DeviceOperator internal;

    @Override
    @SuppressWarnings("all")
    public <T> T unwrap(Class<T> type) {
        if (type == ExternalDeviceOperator.class) {
            return (T) type;
        }
        return internal.unwrap(type);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return internal.isWrapperFor(type) || type == ExternalDeviceOperator.class;
    }

    @Override
    public String getId() {
        return externalId;
    }

    @Override
    public String getDeviceId() {
        return externalId;
    }

    @Override
    public Mono<String> getConnectionServerId() {
        return internal.getConnectionServerId();
    }

    @Override
    public Mono<String> getSessionId() {
        return internal.getSessionId();
    }

    @Override
    public Mono<String> getAddress() {
        return internal.getAddress();
    }

    @Override
    public Mono<Void> setAddress(String address) {
        return internal.setAddress(address);
    }

    @Override
    public Mono<Boolean> putState(byte state) {
        return internal.putState(state);
    }

    @Override
    public Mono<Byte> getState() {
        return internal.getState();
    }

    @Override
    public Mono<Byte> checkState() {
        return internal.checkState();
    }

    @Override
    public Mono<Long> getOnlineTime() {
        return internal.getOnlineTime();
    }

    @Override
    public Mono<Long> getOfflineTime() {
        return internal.getOfflineTime();
    }

    @Override
    public Mono<Boolean> online(String serverId, String sessionId, String address) {
        return internal.online(serverId, sessionId, address);
    }

    @Override
    public Mono<Boolean> online(String serverId, String address, long onlineTime) {
        return internal.online(serverId, address, onlineTime);
    }

    @Override
    public Mono<Value> getSelfConfig(String key) {
        return internal.getSelfConfig(key);
    }

    @Override
    public Mono<Values> getSelfConfigs(Collection<String> keys) {
        return internal.getSelfConfigs(keys);
    }

    @Override
    public Mono<Boolean> offline() {
        return internal.offline();
    }

    @Override
    public Mono<Boolean> disconnect() {
        return internal.disconnect();
    }

    @Override
    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return internal.authenticate(request);
    }

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        return internal.getMetadata();
    }

    @Override
    public Mono<ProtocolSupport> getProtocol() {
        return internal.getProtocol();
    }

    @Override
    public DeviceMessageSender messageSender() {
        return internal.messageSender();
    }

    @Override
    public Mono<Boolean> updateMetadata(String metadata) {
        return internal.updateMetadata(metadata);
    }

    @Override
    public Mono<Boolean> updateMetadata(ThingMetadata metadata) {
        return internal.updateMetadata(metadata);
    }


    @Override
    public Mono<Void> resetMetadata() {
        return internal.resetMetadata();
    }

    @Override
    public Mono<DeviceProductOperator> getProduct() {
        return internal
            .getProduct()
            .flatMap(opt -> idMapper
                .getExternalId(PluginDataIdMapper.TYPE_PRODUCT, pluginId, opt.getId())
                .map(ext -> new ExternalDeviceProductOperator(ext, opt)));
    }

    @Override
    public Mono<DeviceOperator> getParentDevice() {
        return internal
            .getParentDevice()
            .flatMap(device -> idMapper
                .getExternalId(PluginDataIdMapper.TYPE_DEVICE, pluginId, device.getId())
                .map(ext -> new ExternalDeviceOperator(ext, pluginId, idMapper, device)));
    }

    @Override
    public Mono<Value> getConfig(String key) {
        return internal.getConfig(key);
    }

    @Override
    public Mono<Values> getConfigs(Collection<String> keys) {
        return internal.getConfigs(keys);
    }

    @Override
    public Mono<Boolean> setConfig(String key, Object value) {
        return internal.setConfig(key, value);
    }

    @Override
    public Mono<Boolean> setConfigs(Map<String, Object> conf) {
        return internal.setConfigs(conf);
    }

    @Override
    public Mono<Boolean> removeConfig(String key) {
        return internal.removeConfig(key);
    }

    @Override
    public Mono<Value> getAndRemoveConfig(String key) {
        return internal.getAndRemoveConfig(key);
    }

    @Override
    public Mono<Boolean> removeConfigs(Collection<String> key) {
        return internal.removeConfigs(key);
    }

    @Override
    public Mono<Void> refreshConfig(Collection<String> keys) {
        return internal.refreshConfig(keys);
    }

    @Override
    public Mono<Void> refreshAllConfig() {
        return internal.refreshAllConfig();
    }
}
