package org.jetlinks.community.plugin;

import org.jetlinks.plugin.core.PluginDriver;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 插件驱动管理器
 *
 * @author zhouhao
 * @since 2.0
 */
public interface PluginDriverManager {

    /**
     * 获取全部已加载的驱动信息
     *
     * @return 驱动信息
     */
    Flux<PluginDriver> getDrivers();

    /**
     * 根据ID获取驱动信息
     *
     * @param id ID
     * @return 驱动信息
     */
    Mono<PluginDriver> getDriver(String id);

    /**
     * 监听驱动相关事件,可通过返回值{@link Disposable#dispose()} 来取消监听
     *
     * @param listener 监听器
     * @return Disposable
     */
    Disposable listen(PluginDriverListener listener);
}
