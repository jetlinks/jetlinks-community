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
package org.jetlinks.community.reference;

import org.jetlinks.community.strategy.Strategy;
import reactor.core.publisher.Flux;

/**
 * 数据引用提供商,用于提供获取对应类型的引用信息
 *
 * @author zhouhao
 * @since 2.0
 */
public interface DataReferenceProvider extends Strategy {

    /**
     * 引用的数据类型
     *
     * @see DataReferenceManager
     */
    @Override
    String getId();

    /**
     * 获取数据引用信息
     *
     * @param dataId 数据ID
     * @return 引用信息
     */
    Flux<DataReferenceInfo> getReference(String dataId);

    /**
     * 获取全部数据引用信息
     *
     * @return 引用信息
     */
    Flux<DataReferenceInfo> getReferences();
}
