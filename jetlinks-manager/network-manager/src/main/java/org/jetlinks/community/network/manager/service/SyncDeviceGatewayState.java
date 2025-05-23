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

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author wangzheng
 * @since 1.0
 */
@Order(1)
@Component
@Slf4j
public class SyncDeviceGatewayState implements CommandLineRunner {

    private final DeviceGatewayService deviceGatewayService;

    private final DeviceGatewayManager deviceGatewayManager;

    private final Duration gatewayStartupDelay = Duration.ofSeconds(5);

    public SyncDeviceGatewayState(DeviceGatewayService deviceGatewayService, DeviceGatewayManager deviceGatewayManager) {
        this.deviceGatewayService = deviceGatewayService;
        this.deviceGatewayManager = deviceGatewayManager;
    }

    @Override
    public void run(String... args) {
        log.debug("start device gateway in {} later", gatewayStartupDelay);
        Mono.delay(gatewayStartupDelay)
            .then(
                deviceGatewayService
                    .createQuery()
                    .where()
                    .and(DeviceGatewayEntity::getState, NetworkConfigState.enabled)
                    .fetch()
                    .map(DeviceGatewayEntity::getId)
                    .flatMap(deviceGatewayManager::getGateway)
                    .flatMap(DeviceGateway::startup)
                    .onErrorContinue((err, obj) -> {
                        log.error(err.getMessage(), err);
                    })
                    .then()
            )
            .subscribe();
    }
}
