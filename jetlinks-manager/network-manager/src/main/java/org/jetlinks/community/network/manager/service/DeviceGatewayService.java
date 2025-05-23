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

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.jetlinks.community.network.manager.service.DeviceGatewayEventHandler.DO_NOT_RELOAD_GATEWAY;

/**
 * @author wangzheng
 * @since 1.0
 */
@Service
public class DeviceGatewayService extends GenericReactiveCrudService<DeviceGatewayEntity, String> {

    public Mono<Integer> updateState(String id, DeviceGatewayState state) {
        return createUpdate()
            .where()
            .and(DeviceGatewayEntity::getId, id)
            .set(DeviceGatewayEntity::getState, state)
            .set(DeviceGatewayEntity::getStateTime, System.currentTimeMillis())
            .execute()
            .contextWrite(Context.of(DO_NOT_RELOAD_GATEWAY, true));
    }


    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return this
            .findById(Flux.from(idPublisher))
            .switchIfEmpty(Mono.error(new NotFoundException("error.device_gateway_not_exist")))
            .doOnNext(deviceGatewayEntity -> {
                if (DeviceGatewayState.enabled.equals(deviceGatewayEntity.getState())) {
                    throw new UnsupportedOperationException("error.device_gateway_enabled");
                }
            })
            .map(DeviceGatewayEntity::getId)
            .as(super::deleteById);
    }
}
