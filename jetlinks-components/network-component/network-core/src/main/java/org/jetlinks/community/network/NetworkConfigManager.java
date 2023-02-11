package org.jetlinks.community.network;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 网络组件配置管理器
 *
 * @author zhouhao
 */
public interface NetworkConfigManager {
    /**
     * 获取全部的网络配置
     *
     * @return 配置信息
     * @since 2.0
     */
    default Flux<NetworkProperties> getAllConfigs() {
        return Flux.empty();
    }

    Mono<NetworkProperties> getConfig(NetworkType networkType, String id);

}
