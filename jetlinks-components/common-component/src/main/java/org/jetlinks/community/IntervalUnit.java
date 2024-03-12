package org.jetlinks.community;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

/**
 * 时间间隔单位，可用于计算时间范围的间隔周期
 *
 * @author zhouhao
 * @since 1.12
 */
@AllArgsConstructor
public enum IntervalUnit {
    MILLIS(1, ChronoUnit.MILLIS),
    SECONDS(1, ChronoUnit.SECONDS),
    MINUTES(1, ChronoUnit.MINUTES),
    HOURS(1, ChronoUnit.HOURS),
    DAYS(1, ChronoUnit.DAYS),
    WEEKS(1, ChronoUnit.WEEKS) {
        @Override
        protected LocalDateTime doTruncateTo(LocalDateTime time) {
            return time.truncatedTo(ChronoUnit.DAYS)
                       .minusDays(time.getDayOfWeek().getValue() - 1);
        }
    },
    MONTHS(1, ChronoUnit.MONTHS) {
        @Override
        protected LocalDateTime doTruncateTo(LocalDateTime time) {
            return time.truncatedTo(ChronoUnit.DAYS)
                       .withDayOfMonth(1);
        }
    },
    //季度
    QUARTER(3, ChronoUnit.MONTHS) {
        @Override
        protected LocalDateTime doTruncateTo(LocalDateTime time) {
            return time
                .withMonth(time.getMonth().firstMonthOfQuarter().getValue())
                .truncatedTo(ChronoUnit.DAYS)
                .withDayOfMonth(1);

        }
    },
    YEARS(1, ChronoUnit.YEARS) {
        @Override
        protected LocalDateTime doTruncateTo(LocalDateTime time) {
            return time.truncatedTo(ChronoUnit.DAYS)
                       .withDayOfYear(1);
        }
    },
    //不分区,永远返回0
    FOREVER(1, ChronoUnit.FOREVER) {
        @Override
        public long truncatedTo(long timestamp) {
            return 0;
        }

        @Override
        public Iterable<Long> iterate(long from, long to, int duration) {
            return () -> new Iterator<Long>() {
                private boolean nexted;

                @Override
                public boolean hasNext() {
                    return !nexted;
                }

                @Override
                public Long next() {
                    nexted = true;
                    return 0L;
                }
            };
        }
    };

    @Getter
    private final int durationOfUnit;

    @Getter
    private final ChronoUnit unit;

    protected LocalDateTime doTruncateTo(LocalDateTime time) {
        return time.truncatedTo(unit);
    }

    protected LocalDateTime next(LocalDateTime time, int duration) {
        return time.plus((long) durationOfUnit * duration, unit);
    }

    protected long next(long timestamp) {
        return next(timestamp, 1);
    }

    protected long next(long timestamp, int duration) {
        return this
            .next(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC), duration)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

    public long truncatedTo(long timestamp) {
        return this
            .doTruncateTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    }

    public final long truncatedTo(long timestamp, int duration) {
        long ts = truncatedTo(timestamp);
        //指定了多个周期
        if (Math.abs(duration) > 1) {
            ts = next(ts, duration);
        }
        return ts;
    }

    public long toMillis(int duration) {
        return duration * durationOfUnit * unit.getDuration().toMillis();
    }

    public final Iterable<Long> iterate(long from, long to) {
        return iterate(from, to, 1);
    }

    /**
     * 迭代时间区间的每一个周期时间
     *
     * @param from     时间从
     * @param to       时间止
     * @param duration 间隔数量,比如 2天为一个间隔
     * @return 每个间隔的时间戳迭代器
     */
    public Iterable<Long> iterate(long from, long to, int duration) {
        return () -> new Iterator<Long>() {
            long _from = truncatedTo(Math.min(from, to));
            final long _to = truncatedTo(Math.max(from, to));

            @Override
            public boolean hasNext() {
                return _from <= _to;
            }

            @Override
            public Long next() {
                long that = truncatedTo(_from, duration);
                _from = IntervalUnit.this.next(_from, duration);
                return that;
            }
        };
    }

}
