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
