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
 * 已缓冲的数据
 *
 * @param <T> 数据类型
 * @author zhouhao
 * @since 2.2
 */
public interface Buffered<T> {

    /**
     * @return 数据
     */
    T getData();

    /**
     * @return 当前重试次数
     */
    int getRetryTimes();

    /**
     * 标记是否重试此数据
     */
    void retry(boolean retry);

    /**
     * 标记此数据为死信
     */
    void dead();
}
