package org.jetlinks.community.network.resource;

import org.jetlinks.core.cluster.ServerNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 网络资源管理器,用于统一管理网络资源信息.
 * <p>
 * 可通过此接口来获取可用的网络资源信息.
 *
 * @author zhouhao
 * @see NetworkResourceUser
 * @since 2.0
 */
public interface NetworkResourceManager {

    /**
     * 获取集群全部可用的网络资源信息
     *
     * @return 资源信息
     */
    Flux<NetworkResource> getAliveResources();

    /**
     * 判断指定的HOST和端口是否可用
     *
     * @param protocol 网络协议
     * @param host     HOST
     * @param port     端口
     * @return 是否可用
     */
    default Mono<Boolean> isAlive(NetworkTransport protocol, String host, int port) {
        return this
            .getAliveResources()
            .filter(resource -> resource.isSameHost(host) && resource.containsPort(protocol, port))
            .hasElements();
    }

}
