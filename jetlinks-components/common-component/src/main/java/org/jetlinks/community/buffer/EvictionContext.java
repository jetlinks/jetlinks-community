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
package org.jetlinks.community.buffer;

/**
 * 缓冲淘汰上下文
 *
 * @author zhouhao
 * @since 2.0
 */
public interface EvictionContext {

    /**
     * 获取指定类型的数据量
     *
     * @param type 类型
     * @return 数据量
     */
    long size(BufferType type);

    /**
     * 删除最新的数据
     *
     * @param type 类型
     */
    void removeLatest(BufferType type);

    /**
     * 删除最旧的数据
     *
     * @param type 类型
     */
    void removeOldest(BufferType type);

    /**
     * @return 缓冲区名称, 用于区分多个不同的缓冲区
     */
    String getName();

    enum BufferType {
        //缓冲区
        buffer,
        //死数据
        dead
    }
}
