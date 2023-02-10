package org.jetlinks.community.network.resource;

import reactor.core.publisher.Flux;

/**
 * 网络资源使用者,通过实现此接口来定义网络资源使用者.
 * 在获取网络资源使用情况时,会调用{@link NetworkResourceUser#getUsedResources()}来进行获取.
 *
 * @author zhouhao
 * @since 2.0
 */
public interface NetworkResourceUser {

    /**
     * 获取当前节点已使用的的网络资源信息
     *
     * @return 网络资源信息
     */
    Flux<NetworkResource> getUsedResources();

}
