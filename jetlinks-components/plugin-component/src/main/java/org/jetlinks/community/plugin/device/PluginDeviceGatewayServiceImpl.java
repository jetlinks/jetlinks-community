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

import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.plugin.internal.device.PluginDeviceGatewayService;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.plugin.utils.PluginUtils;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.publisher.Mono;

public class PluginDeviceGatewayServiceImpl implements PluginDeviceGatewayService {

    private final DeviceGatewayHelper gatewayHelper;

    private final PluginDataIdMapper dataIdMapper;

    public PluginDeviceGatewayServiceImpl(DeviceRegistry registry,
                                          DeviceSessionManager sessionManager,
                                          DecodedClientMessageHandler handler,
                                          PluginDataIdMapper dataIdMapper) {
        this.gatewayHelper = new DeviceGatewayHelper(registry, sessionManager, handler);
        this.dataIdMapper = dataIdMapper;
    }

    @Override
    public Mono<Void> handleMessage(DeviceGatewayPlugin plugin,
                                    DeviceMessage deviceMessage) {

        return PluginUtils
            .transformToInternalMessage(dataIdMapper, plugin, deviceMessage)
            .flatMap(msg -> handleMessage0(plugin, msg));
    }

    private Mono<Void> handleMessage0(DeviceGatewayPlugin deviceGatewayPlugin,
                                      DeviceMessage deviceMessage) {
        return gatewayHelper
            .handleDeviceMessage(
                deviceMessage,
                PluginDeviceSession::new,
                session -> {
                    if (session.isWrapFrom(PluginDeviceSession.class)) {
                        session
                            .unwrap(PluginDeviceSession.class)
                            .setIdMapper(dataIdMapper);
                        session
                            .unwrap(PluginDeviceSession.class)
                            .setPlugin(deviceGatewayPlugin);
                    }

                }, () -> {

                })
            .then();
    }


}
