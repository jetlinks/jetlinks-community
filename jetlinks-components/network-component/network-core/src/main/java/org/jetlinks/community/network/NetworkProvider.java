package org.jetlinks.community.network;

import org.jetlinks.core.metadata.ConfigMetadata;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 网络组件支持提供商
 *
 * @param <P> 网络组件类型
 */
public interface NetworkProvider<P> {

    /**
     * @return 类型
     * @see DefaultNetworkType
     */
    @Nonnull
    NetworkType getType();

    /**
     * 使用配置创建一个网络组件
     *
     * @param properties 配置信息
     * @return 网络组件
     */
    @Nonnull
    Mono<Network> createNetwork(@Nonnull P properties);

    /**
     * 重新加载网络组件
     *
     * @param network    网络组件
     * @param properties 配置信息
     */
    Mono<Network> reload(@Nonnull Network network, @Nonnull P properties);

    /**
     * @return 配置定义元数据
     */
    @Nullable
    ConfigMetadata getConfigMetadata();

    /**
     * 根据可序列化的配置信息创建网络组件配置
     *
     * @param properties 原始配置信息
     * @return 网络配置信息
     */
    @Nonnull
    Mono<P> createConfig(@Nonnull NetworkProperties properties);

    /**
     * 返回网络组件是否可复用,网络组件不能复用时,在设备接入等操作时将无法选择已经被使用的网络组件.
     * <p>
     * 场景：在设备接入时,像TCP服务等同一个网络组件只能接入一种设备,因此同一个TCP服务是
     * 不能被多个设备接入网关使用的.
     *
     * @return 是否可以复用
     */
    default boolean isReusable() {
        return false;
    }

}
