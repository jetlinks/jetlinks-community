package org.jetlinks.community.plugin;

import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.community.plugin.impl.PluginDriverInstallerProvider;
import reactor.core.publisher.Mono;

/**
 * 插件驱动安装器,用于根据查看驱动配置安装,卸载插件驱动
 *
 * @author zhouhao
 * @see PluginDriverInstallerProvider
 * @since 2.0
 */
public interface PluginDriverInstaller {

    /**
     * 安装驱动
     *
     * @param config 驱动配置
     * @return 驱动
     */
    Mono<PluginDriver> install(PluginDriverConfig config);

    /**
     * 重新加载驱动
     * @param driver 旧驱动
     * @param config 驱动配置
     * @return 驱动
     */
    Mono<PluginDriver> reload(PluginDriver driver,
                              PluginDriverConfig config);

    /**
     * 卸载驱动
     *
     * @param config 驱动配置
     * @return void
     */
    Mono<Void> uninstall(PluginDriverConfig config);

}
