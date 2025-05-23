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
package org.jetlinks.community.network.manager.service;

import lombok.AllArgsConstructor;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 消息协议的数据引用提供商.
 *
 * 返回被设备接入网关使用的消息协议
 *
 * @author zhangji 2022/4/12
 */
@Component
@AllArgsConstructor
public class ProtocolDataReferenceProvider implements DataReferenceProvider {
    private final DeviceGatewayService deviceGatewayService;

    @Override
    public String getId() {
        return DataReferenceManager.TYPE_PROTOCOL;
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String protocolId) {
        return deviceGatewayService
            .createQuery()
            .where()
            .is(DeviceGatewayEntity::getProtocol, protocolId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getProtocol(), DataReferenceManager.TYPE_DEVICE_GATEWAY, e.getId(), e.getName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return deviceGatewayService
            .createQuery()
            .where()
            .notNull(DeviceGatewayEntity::getProtocol)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getProtocol(), DataReferenceManager.TYPE_DEVICE_GATEWAY,  e.getId(), e.getName()));
    }
}
