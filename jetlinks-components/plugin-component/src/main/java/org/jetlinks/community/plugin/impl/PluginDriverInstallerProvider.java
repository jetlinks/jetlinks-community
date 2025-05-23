/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
