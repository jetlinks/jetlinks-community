package org.jetlinks.community.plugin.impl;

import org.jetlinks.community.plugin.PluginDriverInstaller;

/**
 * 插件启动安装器提供商,用于支持不同的插件类型,如: jar等
 *
 * @author zhouhao
 * @since 2.0
 */
public interface PluginDriverInstallerProvider  extends PluginDriverInstaller {

    /**
     * 提供商标识
     * @return 标识
     */
    String provider();

}
