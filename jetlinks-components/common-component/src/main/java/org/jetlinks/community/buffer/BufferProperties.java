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

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class BufferProperties {
    //缓冲文件存储目录
    private String filePath;

    //缓冲区大小,超过此大小将执行 handler 处理逻辑
    private int size = 1000;

    //缓冲超时时间
    private Duration timeout = Duration.ofSeconds(1);

    //并行度,表示支持并行写入的最大线程数.
    private int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors());

    //最大重试次数,超过此次数的数据将会放入死队列.
    private long maxRetryTimes = 64;

    //文件操作的最大并行度,默认为1,不建议设置超过4.
    private int fileConcurrency = 1;

    //消费策略 默认先进先出
    private ConsumeStrategy strategy = ConsumeStrategy.FIFO;

    //淘汰策略
    private BufferEvictionSpec eviction = new BufferEvictionSpec();

    public boolean isExceededRetryCount(int count) {
        return maxRetryTimes > 0 && count >= maxRetryTimes;
    }
}
