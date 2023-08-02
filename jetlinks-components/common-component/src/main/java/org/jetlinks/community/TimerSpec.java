package org.jetlinks.community;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronConstraintsFactory;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.FieldExpressionFactory;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.exception.ValidationException;
import org.reactivestreams.Subscription;
import org.springframework.util.Assert;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
@Setter
public class TimerSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "触发方式")
    @NotNull
    private Trigger trigger;

    //Cron表达式
    @Schema(description = "触发方式为[cron]时不能为空")
    private String cron;

    @Schema(description = "执行的时间.为空则表示每天,触发方式为[week]则为1-7,触发方式为[month]时则为1-31")
    private Set<Integer> when;

    @Schema(description = "执行模式,一次还是周期执行")
    private ExecuteMod mod;

    @Schema(description = "执行模式为[period]时不能为空")
    private Period period;

    @Schema(description = "执行模式为[once]时不能为空")
    private Once once;

    public static TimerSpec cron(String cron) {
        TimerSpec spec = new TimerSpec();
        spec.cron = cron;
        spec.trigger = Trigger.cron;
        return spec;
    }

    public Predicate<LocalDateTime> createRangeFilter() {
        if (CollectionUtils.isEmpty(when)) {
            return ignore -> true;
        }
        if (trigger == Trigger.week) {
            return date -> when.contains(date.getDayOfWeek().getValue());
        } else if (trigger == Trigger.month) {
            return date -> when.contains(date.getDayOfMonth());
        }
        return ignore -> true;
    }

    public Predicate<LocalDateTime> createTimeFilter() {
        Predicate<LocalDateTime> range = createRangeFilter();
        //周期执行指定了to,表示只在时间范围段内执行
        if (mod == ExecuteMod.period) {
            LocalTime to = period.toLocalTime();
            LocalTime from = period.fromLocalTime();
            Predicate<LocalDateTime> predicate
                = time -> time.toLocalTime().compareTo(from) >= 0;
            if (to != null) {
                predicate = predicate.and(time -> time.toLocalTime().compareTo(to) <= 0);
            }
            return predicate.and(range);
        }
        if (mod == ExecuteMod.once) {
            LocalTime onceTime = once.localTime();
            Predicate<LocalDateTime> predicate
                = time -> compareOnceTime(time.toLocalTime(), onceTime) == 0;
            return predicate.and(range);
        }
        return range;
    }

    public int compareOnceTime(LocalTime time1, LocalTime time2) {
        int cmp = Integer.compare(time1.getHour(), time2.getHour());
        if (cmp == 0) {
            cmp = Integer.compare(time1.getMinute(), time2.getMinute());
            if (cmp == 0) {
                cmp = Integer.compare(time1.getSecond(), time2.getSecond());
                //不比较纳秒
            }
        }
        return cmp;
    }

    public String toCronExpression() {
        return toCron().asString();
    }

    private static CronDefinition quartz() {
        return CronDefinitionBuilder
            .defineCron()
            .withSeconds()
            .withValidRange(0, 59)
            .and()
            .withMinutes()
            .withValidRange(0, 59)
            .and()
            .withHours()
            .withValidRange(0, 23)
            .and()
            .withDayOfMonth()
            .withValidRange(1, 31)
            .supportsL()
            .supportsW()
            .supportsLW()
            .supportsQuestionMark()
            .and()
            .withMonth()
            .withValidRange(1, 12)
            .and()
            .withDayOfWeek()
            .withValidRange(1, 7)
            .withMondayDoWValue(1)
            .supportsHash()
            .supportsL()
            .supportsQuestionMark()
            .and()
            .withYear()
            .withValidRange(1970, 2099)
            .withStrictRange()
            .optional()
            .and()
            .withCronValidation(CronConstraintsFactory.ensureEitherDayOfWeekOrDayOfMonth())
            .instance();
    }

    public Cron toCron() {
        CronDefinition definition = quartz();
        if (trigger == Trigger.cron || trigger == null) {
            Assert.hasText(cron, "error.scene_rule_timer_cron_cannot_be_empty");
            return new CronParser(definition).parse(cron).validate();
        }

        CronBuilder builder = CronBuilder.cron(definition);
        builder.withYear(FieldExpression.always());
        builder.withMonth(FieldExpression.always());

        FieldExpression range;
        if (CollectionUtils.isNotEmpty(when)) {
            FieldExpression expr = null;
            for (Integer integer : when) {
                if (expr == null) {
                    expr = FieldExpressionFactory.on(integer);
                } else {
                    expr = expr.and(FieldExpressionFactory.on(integer));
                }
            }
            range = expr;
        } else {
            range = FieldExpressionFactory.questionMark();
        }

        if (trigger == Trigger.week) {
            builder.withDoM(FieldExpressionFactory.questionMark())
                   .withDoW(range);
        } else if (trigger == Trigger.month) {
            builder.withDoM(range)
                   .withDoW(FieldExpressionFactory.questionMark());
        }

        //执行一次
        if (mod == ExecuteMod.once) {
            LocalTime time = once.localTime();
            builder.withHour(FieldExpressionFactory.on(time.getHour()));
            builder.withMinute(FieldExpressionFactory.on(time.getMinute()));
            builder.withSecond(FieldExpressionFactory.on(time.getSecond()));
        }
        //周期执行
        if (mod == ExecuteMod.period) {
            LocalTime time = period.fromLocalTime();
            PeriodUnit unit = period.unit;
            if (unit == PeriodUnit.hours) {
                builder.withHour(FieldExpressionFactory.every(FieldExpressionFactory.on(time.getHour()), period.every))
                       .withMinute(FieldExpressionFactory.on(time.getMinute()))
                       .withSecond(FieldExpressionFactory.on(time.getSecond()));
            } else if (unit == PeriodUnit.minutes) {
                builder
                    .withHour(FieldExpressionFactory.always())
                    .withMinute(FieldExpressionFactory.every(FieldExpressionFactory.on(time.getMinute()), period.every))
                    .withSecond(FieldExpressionFactory.on(time.getSecond()));
            } else if (unit == PeriodUnit.seconds) {
                builder
                    .withHour(FieldExpressionFactory.always())
                    .withMinute(FieldExpressionFactory.always())
                    .withSecond(FieldExpressionFactory.every(FieldExpressionFactory.on(time.getSecond()), period.every));
            }
        }
        return builder.instance().validate();
    }

    public void validate() {
        if (trigger == null) {
            Assert.hasText(cron, "error.scene_rule_timer_cron_cannot_be_empty");
        }
        if (trigger == Trigger.cron) {
            try {
                toCronExpression();
            } catch (Throwable e) {
                ValidationException exception = new ValidationException("cron", "error.cron_format_error", cron);
                exception.addSuppressed(e);
                throw exception;
            }
        } else {
            nextDurationBuilder().apply(ZonedDateTime.now());
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class Once implements Serializable {
        private static final long serialVersionUID = 1L;
        //时间点
        @Schema(description = "时间点.格式:[hh:mm],或者[hh:mm:ss]")
        @NotBlank
        private String time;

        public LocalTime localTime() {
            return parsTime(time);
        }
    }

    @Getter
    @Setter
    public static class Period implements Serializable {
        private static final long serialVersionUID = 1L;
        //周期执行的时间区间
        @Schema(description = "执行时间范围从.格式:[hh:mm],或者[hh:mm:ss]")
        private String from;
        @Schema(description = "执行时间范围止.格式:[hh:mm],或者[hh:mm:ss]")
        private String to;

        @Schema(description = "周期值，如:每[every][unit]执行一次")
        private int every;

        @Schema(description = "周期执行单位")
        private PeriodUnit unit;

        public LocalTime fromLocalTime() {
            return parsTime(from);
        }

        public LocalTime toLocalTime() {
            return parsTime(to);
        }

    }

    private static LocalTime parsTime(String time) {
        return LocalTime.parse(time);
    }

    /**
     * 创建一个下一次执行时间间隔构造器,通过构造器来获取基准时间间隔
     * <pre>{@code
     *
     *   Function<ZonedDateTime, Duration> builder = nextDurationBuilder();
     *
     *   Duration duration =  builder.apply(ZonedDateTime.now());
     *
     * }</pre>
     *
     * @return 构造器
     */
    public Function<ZonedDateTime, Duration> nextDurationBuilder() {
        return nextDurationBuilder(ZonedDateTime.now());
    }


    public Function<ZonedDateTime, Duration> nextDurationBuilder(ZonedDateTime baseTime) {
        Iterator<ZonedDateTime> it = iterable().iterator(baseTime);
        return (time) -> {
            Duration duration;
            do {
                duration = Duration.between(time, time = it.next());
            }
            while (duration.toMillis() < 0);
            return duration;
        };
    }


    /**
     * 创建一个时间构造器,通过构造器来获取下一次时间
     * <pre>{@code
     *
     *   Function<ZonedDateTime, ZonedDateTime> builder = nextTimeBuilder();
     *
     *   ZonedDateTime nextTime =  builder.apply(ZonedDateTime.now());
     *
     * }</pre>
     *
     * @return 构造器
     */
    public Function<ZonedDateTime, ZonedDateTime> nextTimeBuilder() {
        TimerIterable it = iterable();
        return time -> it.iterator(time).next();
    }

    static int MAX_IT_TIMES = 10000;

    private TimerIterable cronIterable() {
        Cron cron = this.toCron();
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Predicate<LocalDateTime> filter = createTimeFilter();
        return baseTime -> new Iterator<ZonedDateTime>() {
            ZonedDateTime current = baseTime;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public ZonedDateTime next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ZonedDateTime dateTime = current;
                int i = 0;
                do {
                    dateTime = executionTime
                        .nextExecution(dateTime)
                        .orElse(null);
                    if (dateTime == null) {
                        i++;
                        continue;
                    }
                    if (filter.test(dateTime.toLocalDateTime())) {
                        break;
                    }
                } while (i < MAX_IT_TIMES);
                return current = dateTime;
            }
        };
    }

    private TimerIterable periodIterable() {
        Assert.notNull(period, "period can not be null");
        Predicate<LocalDateTime> filter = createTimeFilter();

        Duration duration = Duration.of(period.every, period.unit.temporal);
        LocalTime time = period.fromLocalTime();
        return baseTime -> new Iterator<ZonedDateTime>() {
            ZonedDateTime current = baseTime;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public ZonedDateTime next() {
                ZonedDateTime dateTime = current;
                int max = MAX_IT_TIMES;
                do {
                    dateTime = dateTime.plus(duration);
                    if (filter.test(dateTime.toLocalDateTime())) {
                        break;
                    }
                    max--;
                } while (max > 0);

                return current = dateTime;
            }
        };
    }

    private TimerIterable onceIterable() {
        Assert.notNull(once, "once can not be null");
        Predicate<LocalDateTime> filter = createTimeFilter();
        LocalTime onceTime = once.localTime();
        return baseTime -> new Iterator<ZonedDateTime>() {
            ZonedDateTime current = baseTime;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public ZonedDateTime next() {
                ZonedDateTime dateTime = current;
                int max = MAX_IT_TIMES;
                if (!dateTime.toLocalTime().equals(onceTime)) {
                    dateTime = onceTime.atDate(dateTime.toLocalDate()).atZone(dateTime.getZone());
                }
                do {
                    if (filter.test(dateTime.toLocalDateTime()) && current.compareTo(dateTime) <= 0) {
                        current = dateTime.plusDays(1);
                        break;
                    }
                    dateTime = dateTime.plusDays(1);
                    max--;
                } while (max > 0);
                return dateTime;
            }
        };
    }

    public TimerIterable iterable() {
        if ((trigger == Trigger.cron || trigger == null) && cron != null) {
            return cronIterable();
        }
        return mod == ExecuteMod.period ? periodIterable() : onceIterable();
    }

    public List<ZonedDateTime> getNextExecuteTimes(ZonedDateTime from, long times) {
        List<ZonedDateTime> timeList = new ArrayList<>((int) times);
        Iterator<ZonedDateTime> it = iterable().iterator(from);
        for (long i = 0; i < times; i++) {
            timeList.add(it.next());
        }
        return timeList;
    }


    public Flux<Long> flux() {
        return flux(Schedulers.parallel());
    }

    public Flux<Long> flux(Scheduler scheduler) {
        return new TimerFlux(nextDurationBuilder(), scheduler);
    }

    @AllArgsConstructor
    static class TimerFlux extends Flux<Long> {
        final Function<ZonedDateTime, Duration>  spec;
        final Scheduler scheduler;

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super Long> coreSubscriber) {

            TimerSubscriber subscriber = new TimerSubscriber(spec, scheduler, coreSubscriber);
            coreSubscriber.onSubscribe(subscriber);
        }
    }

    static class TimerSubscriber implements Subscription {
        final Function<ZonedDateTime, Duration> spec;
        final CoreSubscriber<? super Long> subscriber;
        final Scheduler scheduler;
        long count;
        Disposable scheduling;

        public TimerSubscriber(Function<ZonedDateTime, Duration> spec,
                               Scheduler scheduler,
                               CoreSubscriber<? super Long> subscriber) {
            this.scheduler = scheduler;
            this.spec = spec;
            this.subscriber = subscriber;
        }


        @Override
        public void request(long l) {
            trySchedule();
        }

        @Override
        public void cancel() {
            if (scheduling != null) {
                scheduling.dispose();
            }
        }

        public void onNext() {

            if (canSchedule()) {
                subscriber.onNext(count++);
            }
            trySchedule();
        }

        void trySchedule() {
            if (scheduling != null) {
                scheduling.dispose();
            }

            ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(scheduler.now(TimeUnit.MILLISECONDS)), ZoneId.systemDefault());
            Duration delay = spec.apply(now);

            scheduling = scheduler
                .schedule(
                    this::onNext,
                    delay.toMillis(),
                    TimeUnit.MILLISECONDS
                );
        }

        protected boolean canSchedule() {
            return true;
        }


    }

    public enum Trigger {
        week,
        month,
        cron
    }

    public enum ExecuteMod {
        period,
        once
    }

    @AllArgsConstructor
    public enum PeriodUnit {
        seconds(ChronoUnit.SECONDS),
        minutes(ChronoUnit.MINUTES),
        hours(ChronoUnit.HOURS);
        private final TemporalUnit temporal;

    }
}
