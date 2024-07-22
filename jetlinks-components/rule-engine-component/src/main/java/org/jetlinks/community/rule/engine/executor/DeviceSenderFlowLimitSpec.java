package org.jetlinks.community.rule.engine.executor;

import com.cronutils.model.time.ExecutionTime;
import com.cronutils.utils.Preconditions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.TimerSpec;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Getter
@Setter
public class DeviceSenderFlowLimitSpec {

    @Schema(description = "自动配置,下发间隔平均分配在轮询周期内,下发总条数为属性个数")
    private boolean autoConfig = false;

    @Schema(description = "每条消息携带的属性数量")
    private int count = 1;

    @Schema(description = "发送消息的间隔,毫秒为单位")
    private long interval;

    @Schema
    private TimerSpec timer;

    /**
     * @param size 轮询总条数
     * @return 轮询间隔，单位毫秒
     */
    public long getExecuteIntervalMillis(int size) {
        if (!autoConfig && interval > 0) {
            return interval;
        }
        ExecutionTime executionTime = ExecutionTime.forCron(timer.toCron());
        ZonedDateTime now = ZonedDateTime.now();
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);
        Preconditions.checkNotNull(nextExecution.orElse(null), "nextExecution");
        Optional<ZonedDateTime> afterNextExecution = executionTime.nextExecution(nextExecution.get());
        Preconditions.checkNotNull(afterNextExecution.orElse(null), "afterNextExecution");
        return ChronoUnit.MILLIS.between(nextExecution.get(), afterNextExecution.get()) / size;
    }
}
