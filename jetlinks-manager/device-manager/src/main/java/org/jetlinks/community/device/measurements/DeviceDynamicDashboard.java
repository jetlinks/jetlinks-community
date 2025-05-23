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
package org.jetlinks.community.device.measurements;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.community.dashboard.DashboardObject;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
public class DeviceDynamicDashboard implements DeviceDashboard {

    private final ReactiveRepository<DeviceProductEntity,String> productRepository;

    private final DeviceRegistry registry;

    private final EventBus eventBus;

    private final DeviceDataService dataService;

    public DeviceDynamicDashboard(ReactiveRepository<DeviceProductEntity,String> productRepository,
                                  DeviceRegistry registry,
                                  DeviceDataService deviceDataService,
                                  EventBus eventBus) {
        this.productRepository = productRepository;
        this.registry = registry;
        this.eventBus = eventBus;
        this.dataService = deviceDataService;
    }

    @PostConstruct
    public void init() {
        //设备状态变更
    }

    @Override
    public Flux<DashboardObject> getObjects() {
        return productRepository
            .createQuery()
            .fetch()
            .flatMap(this::convertObject);
    }

    @Override
    public Mono<DashboardObject> getObject(String id) {
        return productRepository
            .findById(id)
            .flatMap(this::convertObject);
    }

    protected Mono<DeviceDashboardObject> convertObject(DeviceProductEntity product) {
        return registry
            .getProduct(product.getId())
            .map(operator -> DeviceDashboardObject.of(product.getId(), product.getName(), operator, eventBus, dataService,registry));
    }
}
