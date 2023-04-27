package org.jetlinks.community.gateway;

import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.channel.ChannelProvider;
import reactor.core.publisher.Mono;

import java.util.List;
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
}
