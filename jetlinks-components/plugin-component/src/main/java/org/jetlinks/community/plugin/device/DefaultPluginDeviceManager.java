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
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.plugin.internal.device.PluginDeviceManager;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import org.jetlinks.community.plugin.utils.PluginUtils;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.device.DeviceCommandSupportTypes;
import org.jetlinks.sdk.server.device.DeviceInfo;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.function.Consumer;

@AllArgsConstructor
public class DefaultPluginDeviceManager implements PluginDeviceManager {

    private final DeviceRegistry deviceRegistry;
    private final PluginDataIdMapper dataIdMapper;


    @Override
    public Flux<DeviceOperator> getDevices(DeviceGatewayPlugin plugin) {
        return doQuery(q -> applyQuery(plugin, q))
            .flatMap(e -> deviceRegistry
                         .getDevice(e.getId())
                         .flatMap(device -> PluginUtils.transformToExternalDevice(dataIdMapper, plugin, device))
                         .subscribeOn(Schedulers.parallel()),
                     32);
    }

    @Override
    public Flux<DeviceOperator> getDevices(DeviceGatewayPlugin plugin, String productId) {
        return dataIdMapper
            .getInternalId(PluginDataIdMapper.TYPE_PRODUCT, plugin.getId(), productId)
            .flatMapMany(internalId -> doQuery(q -> applyQuery(plugin, q.and("productId", internalId))))
            .flatMap(info -> deviceRegistry
                .getDevice(info.getId())
                .flatMap(device -> PluginUtils.transformToExternalDevice(dataIdMapper, plugin, device))
                .subscribeOn(Schedulers.parallel()))
            ;
    }

    private Flux<DeviceInfo> doQuery(Consumer<Query<?, QueryParamEntity>> customizer) {
        return CommandSupportManagerProviders
            .getProviderNow(SdkServices.deviceService)
            .getCommandSupport(DeviceCommandSupportTypes.device, Collections.emptyMap())
            .flatMapMany(cmd -> cmd.execute(
                QueryListCommand
                    .of(DeviceInfo.class)
                    .dsl(customizer)));
    }

    private void applyQuery(DeviceGatewayPlugin plugin,
                            Query<?, QueryParamEntity> query) {
        query
            .select("id")
            .noPaging()
            .accept("productId",
                    "product-info",
                    QueryParamEntity
                        .of()
                        .toQuery()
                        .is("accessId", plugin.getId())
                        .getParam()
                        .getTerms());
    }
}