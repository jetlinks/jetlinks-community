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
package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class DeviceCurrentStateSubscriptionProvider implements SubscriptionProvider {

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;

    @Override
    @Generated
    public String id() {
        return "device-state-subscriber";
    }

    @Override
    @Generated
    public String name() {
        return "设备当前状态消息";
    }

    @Override
    @Generated
    public String[] getTopicPattern() {
        return new String[]{
            "/device-current-state"
        };
    }

    @Override
    @SuppressWarnings("all")
    @Generated
    public Flux<Map<String, Object>> subscribe(SubscribeRequest request) {
        List<Object> deviceId = request
            .get("deviceId")
            .map(CastUtils::castArray)
            .orElseThrow(() -> new IllegalArgumentException("error.deviceId_cannot_be_empty"));

        return Flux
            .fromIterable(deviceId)
            .buffer(200)
            .concatMap(buf -> {
                return deviceRepository
                    .createQuery()
                    .select(DeviceInstanceEntity::getId, DeviceInstanceEntity::getState)
                    .in(DeviceInstanceEntity::getId, buf)
                    .fetch();
            })
            .map(instance -> Collections.singletonMap(instance.getId(), instance.getState().name()));

    }
}
