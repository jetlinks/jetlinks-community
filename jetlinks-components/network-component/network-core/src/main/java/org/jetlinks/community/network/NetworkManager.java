package org.jetlinks.community.network;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网络服务管理器
 * <p>
 * 管理所有的网络组件
 *
 * @author zhouhao
 * @since 1.0
 */
public interface NetworkManager {

    /**
     * 根据ID获取网络组件，否则根据type和id创建网络组件并返回
     *
     * @param type 网络类型
     * @param id   网络组件id
     * @param <T>  NetWork子类泛型
     * @return 网络组件
     */
    <T extends Network> Mono<T> getNetwork(NetworkType type, String id);

    /**
     * 获取所有的网络组件支持提供商
     *
     * @return 网络组件支持提供商
     */
    List<NetworkProvider<?>> getProviders();

    /**
     * 重新加载网络组件
     *
     * @param type 网络类型
     * @param id   网络组件ID
     * @return void
     */
    Mono<Void> reload(NetworkType type, String id);

    /**
     * 停止网络组件
     *
     * @param type 网络类型
     * @param id   网络组件ID
     * @return void
     */
    Mono<Void> shutdown(NetworkType type, String id);
}
