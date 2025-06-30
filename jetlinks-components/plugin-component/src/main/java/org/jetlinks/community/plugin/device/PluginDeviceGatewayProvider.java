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

import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.server.session.DeviceSessionProvider;
import org.jetlinks.core.server.session.DeviceSessionProviders;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.plugin.core.PluginRegistry;
import org.jetlinks.plugin.core.ServiceRegistry;
import org.jetlinks.plugin.internal.PluginDataIdMapper;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.plugin.internal.device.PluginDeviceGatewayService;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.plugin.PluginDriverManager;
import org.jetlinks.community.plugin.context.*;
import org.jetlinks.community.plugin.monitor.PluginMonitorHelper;
import org.jetlinks.community.plugin.utils.PluginUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PluginDeviceGatewayProvider extends CompositeProtocolSupport
    implements DeviceGatewayProvider, DeviceSessionProvider {

    public static final String CHANNEL_PLUGIN = PluginTransport.plugin.getId();

    public static final String PROVIDER = "plugin_gateway";
    private final PluginRegistry registry;
    private final PluginDriverManager driverManager;
    private final ServiceRegistry serviceRegistry;
    private final PluginDataIdMapper idMapper;
    private final EventBus eventBus;

    private final PluginDeviceGatewayService gatewayService;

    private final DeviceRegistry deviceRegistry;

    private final Map<String, DeviceGatewayPlugin> plugins = new ConcurrentHashMap<>();

    public PluginDeviceGatewayProvider(PluginRegistry registry,
                                       PluginDriverManager driverManager,
                                       ServiceRegistry serviceRegistry,
                                       PluginDataIdMapper idMapper,
                                       DeviceRegistry deviceRegistry,
                                       PluginDeviceGatewayService gatewayService,
                                       EventBus eventBus) {
        this.registry = registry;
        this.deviceRegistry = deviceRegistry;
        this.driverManager = driverManager;
        this.serviceRegistry = serviceRegistry;
        this.idMapper = idMapper;
        this.gatewayService = gatewayService;
        this.eventBus = eventBus;
        setId(PROVIDER);
        setName("插件设备接入");

        addMessageCodecSupport(new PluginMessageCodec());

        //状态检查
        setDeviceStateChecker(
            device -> device
                .getConfig(PropertyConstants.accessId)
                .mapNotNull(plugins::get)
                .flatMap(plugin -> {
                    //转换为插件侧的设备操作接口
                    return PluginUtils
                        .transformToExternalDevice(idMapper, plugin, device)
                        .flatMap(plugin::getDeviceState);
                })
        );

        //监听设备注册
        doOnDeviceRegister(device -> device
            .getConfig(PropertyConstants.accessId)
            .mapNotNull(plugins::get)
            .flatMap(plugin -> {
                //转换为插件侧的设备操作接口
                return PluginUtils
                    .transformToExternalDevice(idMapper, plugin, device)
                    .flatMap(plugin::doOnDeviceRegister);
            }));

        //监听设备注销
        doOnDeviceUnRegister(device -> device
            .getConfig(PropertyConstants.accessId)
            .mapNotNull(plugins::get)
            .flatMap(plugin -> {
                //转换为插件侧的设备操作接口
                return PluginUtils
                    .transformToExternalDevice(idMapper, plugin, device)
                    .flatMap(plugin::doOnDeviceUnregister);
            }));


        //监听产品注册
        doOnProductRegister(product -> product
            .getConfig(PropertyConstants.accessId)
            .mapNotNull(plugins::get)
            .flatMap(plugin -> {
                //转换为插件侧的设备操作接口
                return PluginUtils
                    .transformToExternalProduct(idMapper, plugin, product)
                    .flatMap(plugin::doOnProductRegister);
            }));
        //session序列化
        DeviceSessionProviders.register(this);
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("device.gateway.provider.plugin_gateway.name", super.getName());
    }

    @Override
    public String getDescription() {
        return LocaleUtils.resolveMessage("device.gateway.provider.plugin_gateway.description", super.getDescription());
    }

    public DeviceGatewayPlugin getPlugin(String id) {
        return plugins.get(id);
    }

    @Override
    public Transport getTransport() {
        return PluginTransport.plugin;
    }

    @SuppressWarnings("all")
    private File createWorkdir(String id) {
        File workDir = new File("./data/plugins/device-gateway/" + id);
        if (!workDir.exists()) {
            workDir.mkdirs();
        }
        return workDir;
    }

    @Override
    public String getChannel() {
        return CHANNEL_PLUGIN;
    }


    private Mono<DeviceGateway> createGateway(DeviceGatewayProperties properties,
                                              PluginDriver driver) {

        Monitor monitor = PluginMonitorHelper.createMonitor(eventBus,driver.getType().getId(), properties.getId());

        ClusterPluginScheduler scheduler = new ClusterPluginScheduler(properties.getId(), monitor);

        return driver
            .createPlugin(
                properties.getId(),
                DefaultPluginContext.of(
                    registry,
                    new CompositeServiceRegistry(
                        Arrays.asList(
                            //在插件中获取注册中心时自动转换ID
                            new SingleServiceRegistry("deviceRegistry",
                                                      new ExternalDeviceRegistry(properties.getId(), idMapper, deviceRegistry)),
                            //命令服务
                            CommandServiceRegistry.instance(),
                            //获取其他服务时使用默认的服务注册中心
                            serviceRegistry
                        )
                    ),
                    new SimplePluginEnvironment(properties),
                    monitor,
                    scheduler,
                    createWorkdir(properties.getId())
                ))
            .map(plugin -> {
                DeviceGatewayPlugin gatewayPlugin = plugin.unwrap(DeviceGatewayPlugin.class);
                PluginDeviceGateway gateway = new PluginDeviceGateway(gatewayPlugin.getId(), gatewayPlugin);

                plugins.put(gatewayPlugin.getId(), gatewayPlugin);
                //停止网关时停止所有调度任务
                gateway.doOnShutdown(scheduler);
                //移除网关
                gateway.doOnShutdown(() -> plugins.remove(gatewayPlugin.getId(), gatewayPlugin));

                return gateway;
            });
    }

    @Override
    public Mono<? extends DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {

        return Mono.defer(() -> driverManager
            .getDriver(properties.getChannelId())
            .switchIfEmpty(Mono.error(() -> new BusinessException("error.plugin_driver_does_not_exist", properties.getChannelId())))
            .flatMap(driver -> createGateway(properties, driver)));

    }

    @Override
    public Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway,
                                                             DeviceGatewayProperties properties) {
        return gateway
            .unwrap(PluginDeviceGateway.class)
            .shutdown()
            .then(createDeviceGateway(properties))
            .flatMap(gate -> gate.startup().thenReturn(gate));
    }

    @Override
    public Mono<PersistentSession> deserialize(byte[] sessionData, DeviceRegistry registry) {

        return Mono
            .fromCallable(() -> {
                try (ObjectInput input = Serializers
                    .getDefault()
                    .createInput(new ByteArrayInputStream(sessionData))) {
                    return PluginDeviceSession.read(input, registry, idMapper,plugins::get);
                }
            })
            .flatMap(Function.identity());
    }


    @Override
    public Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry) {
        if (!session.isWrapFrom(PluginDeviceSession.class)) {
            return Mono.empty();
        }
        PluginDeviceSession deviceSession = session.unwrap(PluginDeviceSession.class);

        return Mono.fromCallable(() -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream(70);
            try (ObjectOutput output = Serializers.getDefault().createOutput(out)) {
                deviceSession.write(output);
            }
            return out.toByteArray();
        });
    }

    class PluginMessageCodec implements DeviceMessageCodec {

        @Override
        public Transport getSupportTransport() {
            return PluginTransport.plugin;
        }

        @Nonnull
        @Override
        public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
            //never happen
            return Mono.empty();
        }

        @Nonnull
        @Override
        public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
            DeviceOperator device = context.getDevice();

            Message message = context.getMessage();

            if (null == device || !(message instanceof DeviceMessage)) {
                return Mono.empty();
            }

            return context
                .reply(
                    device
                        // 设备接入网关ID就是插件ID
                        .getConfig(PropertyConstants.accessId)
                        .mapNotNull(plugins::get)
                        .switchIfEmpty(Mono.error(() -> new DeviceOperationException
                            .NoStackTrace(ErrorCode.SERVER_NOT_AVAILABLE, "error.plugin_not_found")))
                        .flatMapMany(plugin -> PluginUtils
                            .transformToExternalMessage(idMapper, plugin, ((DeviceMessage) message).copy())
                            .flatMapMany(plugin.unwrap(DeviceGatewayPlugin.class)::execute)
                            .flatMap(reply -> PluginUtils.transformToInternalMessage(idMapper, plugin, reply.copy())))
                )
                .then(Mono.empty());
        }
    }

}
