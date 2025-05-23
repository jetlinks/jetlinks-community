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
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
public class ExternalDeviceProductOperator implements DeviceProductOperator {

    private final String externalId;

    private final DeviceProductOperator internal;

    @Override
    public String getId() {
        return externalId;
    }

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        //todo 自动转换映射？
        return internal.getMetadata();
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
    public Mono<ProtocolSupport> getProtocol() {
        return internal.getProtocol();
    }

    @Override
    public Flux<DeviceOperator> getDevices() {
        return internal.getDevices();
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
        return internal.setConfig(key,value);
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
