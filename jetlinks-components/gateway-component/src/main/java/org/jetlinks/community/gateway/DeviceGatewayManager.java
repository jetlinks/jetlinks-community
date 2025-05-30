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
package org.jetlinks.community.gateway;

import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.channel.ChannelProvider;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 设备接入网关管理器,统一管理设备接入网关等相关信息
 *
 * @author zhouhao
 * @since 1.0
 */
public interface DeviceGatewayManager {

    /**
     * 获取接入网关
     *
     * @param id ID
     * @return 接入网关
     */
    Mono<DeviceGateway> getGateway(String id);

    /**
     * 重新加载网关
     *
     * @param gatewayId 网关ID
     * @return void
     * @since 2.0
     */
    Mono<Void> reload(String gatewayId);

    /**
     * 停止网关
     *
     * @param gatewayId 网关ID
     * @return void
     */
    Mono<Void> shutdown(String gatewayId);

    /**
     * 启动网关
     *
     * @param id 网关ID
     * @return void
     */
    Mono<Void> start(String id);

    /**
     * 重新加载当前节点的网关
     *
     * @param gatewayId 网关ID
     * @return void
     * @since 2.2
     */
    Mono<Void> reloadLocal(String gatewayId);


    /**
     * 获取接入网关通道信息,通道中包含接入地址等信息
     *
     * @param channel   通道标识，如 network
     * @param channelId 通道ID
     * @return 通道信息
     * @see ChannelProvider#getChannel()
     * @see 2.0
     */
    Mono<ChannelInfo> getChannel(String channel, String channelId);

    /**
     * 获取全部的设备接入网关提供商
     *
     * @return 设备接入网关提供商
     * @see DeviceGatewayProvider
     */
    List<DeviceGatewayProvider> getProviders();

    /**
     * 根据接入提供商标识获取设备接入提供商接口
     *
     * @param provider 提供商标识
     * @return DeviceGatewayProvider
     */
    Optional<DeviceGatewayProvider> getProvider(String provider);

    /**
     * 执行指定网关的指定命令,网关需要实现{@link CommandSupport}.
     *
     * @param gatewayId 网关ID
     * @param commandId 命令ID {@link CommandSupport}
     * @param body      命令参数
     * @return 执行结果
     */
    @SuppressWarnings("all")
    default <T> Mono<T> executeCommand(String gatewayId,
                                       String commandId,
                                       Mono<Map<String, Object>> body) {
        return (Mono)Mono
            .zip(
                this.getGateway(gatewayId)
                    .filter(gateway -> gateway.isWrapperFor(CommandSupport.class))
                    .cast(CommandSupport.class),
                body,
                (cmd, param) -> cmd.execute(cmd.createCommand(commandId).with(param))
            )
            .flatMap(val -> {
                if (val instanceof Mono) {
                    return ((Mono<?>) val);
                }
                if (val instanceof Flux) {
                    return ((Flux<?>) val).collectList();
                }
                return Mono.just(val);
            });
    }
}
