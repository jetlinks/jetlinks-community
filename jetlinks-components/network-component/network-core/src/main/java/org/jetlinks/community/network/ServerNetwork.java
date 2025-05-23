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
package org.jetlinks.community.network;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * 网络服务
 *
 * @author zhouhao
 * @since 1.2
 */
public interface ServerNetwork extends Network {

    /**
     * 获取网络服务绑定的套接字端口信息
     *
     * @return InetSocketAddress
     */
    @Nullable
    InetSocketAddress getBindAddress();

}
