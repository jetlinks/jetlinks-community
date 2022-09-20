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
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * @author zhouhao
 * @since 2.0
 */
@Getter
@AllArgsConstructor
public class BufferSettings {

    private static final Predicate<Throwable> DEFAULT_RETRY_WHEN_ERROR =
        e -> ErrorUtils.hasException(e, IOException.class,
                                     TimeoutException.class,
                                     DataAccessResourceFailureException.class,
                                     CannotCreateTransactionException.class,
                                     QueryTimeoutException.class);

    public static Predicate<Throwable> defaultRetryWhenError() {
        return DEFAULT_RETRY_WHEN_ERROR;
    }

    private final String filePath;

    private final String fileName;

    private final Predicate<Throwable> retryWhenError;

    //缓冲区大小,超过此大小将执行 handler 处理逻辑
    private final int bufferSize;

    //缓冲超时时间
    private final Duration bufferTimeout;

    //并行度,表示支持并行写入的最大线程数.
    private final int parallelism;

    //最大重试次数,超过此次数的数据将会放入死队列.
    private final long maxRetryTimes;


    public static BufferSettings create(String filePath, String fileName) {
        return new BufferSettings(
            filePath,
            fileName,
            defaultRetryWhenError(),
            1000,
            Duration.ofSeconds(1),
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            5);
    }

    public static BufferSettings create(BufferProperties properties) {
        return create("buffer.queue", properties);
    }

    public static BufferSettings create(String fileName, BufferProperties properties) {
        return create(properties.getFilePath(), fileName).properties(properties);
    }

    public BufferSettings bufferSize(int bufferSize) {
        return new BufferSettings(filePath,
                                  fileName,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes);
    }

    public BufferSettings bufferTimeout(Duration bufferTimeout) {
        return new BufferSettings(filePath,
                                  fileName,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes);
    }

    public BufferSettings parallelism(int parallelism) {
        return new BufferSettings(filePath,
                                  fileName,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes);
    }

    public BufferSettings maxRetry(int maxRetryTimes) {
        return new BufferSettings(filePath,
                                  fileName,
                                  retryWhenError,
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes);
    }

    public BufferSettings retryWhenError(Predicate<Throwable> retryWhenError) {
        return new BufferSettings(filePath,
                                  fileName,
                                  Objects.requireNonNull(retryWhenError),
                                  bufferSize,
                                  bufferTimeout,
                                  parallelism,
                                  maxRetryTimes);
    }

    public BufferSettings properties(BufferProperties properties) {
        return new BufferSettings(filePath,
                                  fileName,
                                  Objects.requireNonNull(retryWhenError),
                                  properties.getSize(),
                                  properties.getTimeout(),
                                  properties.getParallelism(),
                                  properties.getMaxRetryTimes());
    }


}
