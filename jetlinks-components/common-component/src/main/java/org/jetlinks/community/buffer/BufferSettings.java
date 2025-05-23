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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.utils.ErrorUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class BufferSettings {

    private static final Predicate<Throwable> DEFAULT_RETRY_WHEN_ERROR =
        e -> ErrorUtils.hasException(e, IOException.class,
                                     IllegalStateException.class,
                                     RejectedExecutionException.class,
                                     TimeoutException.class,
                                     DataAccessResourceFailureException.class,
                                     CannotCreateTransactionException.class,
                                     QueryTimeoutException.class);

    public static Predicate<Throwable> defaultRetryWhenError() {
        return DEFAULT_RETRY_WHEN_ERROR;
    }

    public static BufferEviction defaultEviction(){
        return BufferEvictionSpec.DEFAULT;
    }

    private final String filePath;

    private final String fileName;

    //缓存淘汰策略
    private final BufferEviction eviction;

    private final Predicate<Throwable> retryWhenError;

    //缓冲区大小,超过此大小将执行 handler 处理逻辑
    private final int bufferSize;

    //缓冲超时时间
    private final Duration bufferTimeout;

    //并行度,表示支持并行写入的最大线程数.
    private final int parallelism;

    //最大重试次数,超过此次数的数据将会放入死队列.
    private final long maxRetryTimes;

    private final int fileConcurrency;

    private final ConsumeStrategy strategy;

    public static BufferSettings create(String filePath, String fileName) {
        return new BufferSettings(
            filePath,
            fileName,
            defaultEviction(),
            //默认重试逻辑
            defaultRetryWhenError(),
            1000,
            Duration.ofSeconds(1),
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            5,
            1,
            ConsumeStrategy.FIFO);
    }

    public static BufferSettings create(BufferProperties properties) {
        return create("buffer.queue", properties);
    }

    public static BufferSettings create(String fileName, BufferProperties properties) {
        return create(properties.getFilePath(), fileName).properties(properties);
    }

    public BufferSettings eviction(BufferEviction eviction) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings bufferSize(int bufferSize) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings bufferTimeout(Duration bufferTimeout) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings parallelism(int parallelism) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings maxRetry(int maxRetryTimes) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings retryWhenError(Predicate<Throwable> retryWhenError) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  Objects.requireNonNull(retryWhenError),
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings fileConcurrency(int fileConcurrency) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings strategy(ConsumeStrategy strategy) {
        return new BufferSettings(filePath,
                                  fileName,
                                  eviction,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes,
                                  fileConcurrency,
                                  strategy);
    }

    public BufferSettings properties(BufferProperties properties) {
        return new BufferSettings(filePath,
                                  fileName,
                                  properties.getEviction().build(),
                                  Objects.requireNonNull(retryWhenError),
                                  properties.getSize(),
                                  properties.getTimeout(),
                                  properties.getParallelism(),
                                  properties.getMaxRetryTimes(),
                                  properties.getFileConcurrency(),
                                  properties.getStrategy()
                                  );
    }


}
