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
