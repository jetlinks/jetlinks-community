package org.jetlinks.community.plugin.device;

import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.plugin.internal.device.PluginDeviceGatewayService;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
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
                            .setPlugin(deviceGatewayPlugin);
                    }

                }, () -> {

                })
            .then();
    }


}
