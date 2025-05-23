package org.jetlinks.community.plugin;

import org.jetlinks.plugin.core.PluginDriver;
import reactor.core.publisher.Mono;

/**
 * 插件驱动监听器
 *
 * @author zhouhao
 * @since 2.1
 */
public interface PluginDriverListener {

    /**
     * 当插件安装时触发
     *
     * @param driverId 驱动ID
     * @param driver   驱动
     * @return void
     */
    default Mono<Void> onInstall(String driverId,
                                 PluginDriver driver) {
        return Mono.empty();
    }

    /**
     * 当插件重新加载时触发
     *
     * @param driverId 驱动ID
     * @param driver   驱动
     * @return void
     */
    default Mono<Void> onReload(String driverId,
                                PluginDriver oldDriver,
                                PluginDriver driver) {
        return Mono.empty();
    }

    /**
     * 当插件卸载时触发
     *
     * @param driverId 驱动ID
     * @param driver   驱动
     * @return void
     */
    default Mono<Void> onUninstall(String driverId,
                                   PluginDriver driver) {
        return Mono.empty();
    }

    /**
     * 当插件管理器启动完成时触发
     *
     * @return void
     */
    default Mono<Void> onStartup() {
        return Mono.empty();
    }


}
