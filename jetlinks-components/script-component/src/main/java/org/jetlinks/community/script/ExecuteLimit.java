package org.jetlinks.community.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteLimit {

    protected boolean enabled = true;

    //执行超时时间
    protected Duration executeTimeout;

    //循环最大执行次数,控制可能发生的死循环
    protected long maxLoopTimes;

    public static ExecuteLimit disabled() {
        return new ExecuteLimit(false, null, -1);
    }

    public static ExecuteLimit defaultLimit() {
        return ExecuteLimit
            .builder()
            .enabled(true)
            .executeTimeout(Duration.ofSeconds(5))
            .maxLoopTimes(-1)
            .build();
    }

    public static ExecuteLimitBuilder builder() {
        return new ExecuteLimitBuilder();
    }

    public static class ExecuteLimitBuilder {
        private boolean enabled = true;
        private Duration executeTimeout;
        private long maxLoopTimes;

        ExecuteLimitBuilder() {
        }

        public ExecuteLimitBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ExecuteLimitBuilder executeTimeout(Duration executeTimeout) {
            this.executeTimeout = executeTimeout;
            return this;
        }

        public ExecuteLimitBuilder maxLoopTimes(long maxLoopTimes) {
            this.maxLoopTimes = maxLoopTimes;
            return this;
        }

        public ExecuteLimit build() {
            return new ExecuteLimit(enabled, executeTimeout, maxLoopTimes);
        }

    }
}
