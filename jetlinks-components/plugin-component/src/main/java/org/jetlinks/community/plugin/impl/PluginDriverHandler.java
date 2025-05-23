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
package org.jetlinks.community.plugin.impl;

import org.hswebframework.web.crud.events.EntityBeforeDeleteEvent;
import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.plugin.internal.InternalPluginType;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.plugin.PluginDriverListener;
import org.jetlinks.community.plugin.PluginDriverManager;
import org.jetlinks.community.reference.DataReferenceManager;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 插件事件.
 * 处理设备接入网关相关的联动
 *
 * @author zhangji 2023/4/19
 */
public class PluginDriverHandler implements PluginDriverListener {

    private final DeviceGatewayManager deviceGatewayManager;

    private final DataReferenceManager referenceManager;

    public PluginDriverHandler(DeviceGatewayManager deviceGatewayManager,
                               DataReferenceManager referenceManager,
                               PluginDriverManager driverManager) {
        this.deviceGatewayManager = deviceGatewayManager;
        this.referenceManager = referenceManager;
        driverManager.listen(this);
    }

    @Override
    public Mono<Void> onInstall(String driverId, PluginDriver driver) {
        if(InternalPluginType.deviceGateway.eq(driver.getType().getId())){
            return reloadGateway(driverId);
        }
        return PluginDriverListener.super.onInstall(driverId, driver);
    }

    @Override
    public Mono<Void> onReload(String driverId, PluginDriver oldDriver, PluginDriver driver) {
        if(InternalPluginType.deviceGateway.eq(driver.getType().getId())){
            return reloadGateway(driverId);
        }
        return PluginDriverListener.super.onReload(driverId, oldDriver, driver);
    }

    @EventListener
    public void handleDeleteBefore(EntityBeforeDeleteEvent<PluginDriverEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> referenceManager
                    .assertNotReferenced(
                        DataReferenceManager.TYPE_PLUGIN, entity.getId(), "error.plugin_driver_referenced"
                    ))
        );
    }

    private Mono<Void> reloadGateway(String pluginId) {
        return referenceManager
            .getReferences(DataReferenceManager.TYPE_PLUGIN, pluginId)
            .flatMap(referenceInfo -> deviceGatewayManager.reloadLocal(referenceInfo.getReferenceId()))
            .then();
    }
}
