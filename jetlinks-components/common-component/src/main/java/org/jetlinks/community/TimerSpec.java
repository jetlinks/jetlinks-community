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
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.terms.I18nSpec;
import org.reactivestreams.Subscription;
import org.springframework.util.Assert;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
@Setter
public class TimerSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "触发方式")
    @NotNull
    private Trigger trigger;

    @Schema(description = "使用日程标签进行触发")
    private Set<String> scheduleTags;

    //Cron表达式
    @Schema(description = "触发方式为[cron]时不能为空")
    private String cron;

    @Schema(description = "执行的时间.为空则表示每天,触发方式为[week]则为1-7,触发方式为[month]时则为1-31")
    private Set<Integer> when;

    @Schema(description = "执行模式,一次还是周期执行")
    private ExecuteMod mod;

    @Schema(description = "执行模式为[period]时不能为空")
    private Period period;

    @Schema(description = "执行模式为[period]时不能与period同时为空")
    private List<Period> periods;

    @Schema(description = "执行模式为[once]时不能为空")
    private Once once;

    @Schema(description = "组合触发配置列表")
    private Multi multi;

    public static TimerSpec cron(String cron) {
        TimerSpec spec = new TimerSpec();
        spec.cron = cron;
        spec.trigger = Trigger.cron;
        return spec;
    }

    public List<Period> periods() {
        List<Period> list = new ArrayList<>(1);
        if (periods != null) {
            list.addAll(periods);
        }
        if (period != null) {
            list.add(period);
        }
        return list;
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
        if (mod == ExecuteMod.period) {
            Predicate<LocalDateTime> predicate = null;
            //可能多个周期
            for (Period period : periods()) {
                //周期执行指定了to,表示只在时间范围段内执行
                LocalTime to = period.toLocalTime();
                LocalTime from = period.fromLocalTime();
                Predicate<LocalDateTime> _predicate = time -> !time.toLocalTime().isBefore(from);
                if (to != null) {
                    _predicate = _predicate.and(time -> !time.toLocalTime().isAfter(to));
                }
                if (predicate == null) {
                    predicate = _predicate;
                } else {
                    predicate = _predicate.or(predicate);
                }
            }
            if (predicate == null) {
                return range;
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
        } else if (trigger == Trigger.multi) {
            List<TimerSpec> multiSpec = multi.getSpec();
            if (CollectionUtils.isNotEmpty(multiSpec)) {
                for (TimerSpec spec : multiSpec) {
                    spec.validate();
                }
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

    @Getter
    @Setter
    public static class Multi implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "组合触发配置列表")
        private List<TimerSpec> spec;

        @Schema(description = "多个触发的关系。and、or")
        private Term.Type type = Term.Type.or;

        @Schema(description = "组合触发时，只触发一次的最小时间间隔")
        private int timeSpanSecond = 2;
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
        List<Period> periods = periods();
        Assert.notEmpty(periods, "period or periods can not be null");
        Predicate<LocalDateTime> filter = createTimeFilter();

        Duration _Duration = null;
        LocalTime earliestFrom = LocalTime.MAX;
        LocalTime latestTo = LocalTime.MIN;

        //使用最小的执行周期进行判断?
        for (Period period : periods) {
            Duration duration = Duration.of(period.every, period.unit.temporal);
            LocalTime from = period.fromLocalTime();
            LocalTime to = period.toLocalTime();

            // 更新最小的duration
            if (_Duration == null || duration.compareTo(_Duration) < 0) {
                _Duration = duration;
            }

            // 更新最早的起始时间
            if (from != null && from.isBefore(earliestFrom)) {
                earliestFrom = from;
            }

            // 更新最晚的结束时间
            if (to != null && to.isAfter(latestTo)) {
                latestTo = to;
            }
        }

        Duration duration = _Duration;
        LocalTime firstFrom = earliestFrom.equals(LocalTime.MAX) ? LocalTime.MIDNIGHT : earliestFrom;
        LocalTime endTo = latestTo.equals(LocalTime.MIN) ? null : latestTo;

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
                    // 检查时间是否在 firstFrom 和 endTo 之间
                    LocalTime time = dateTime.toLocalTime();
                    if (time.isBefore(firstFrom) || time.isAfter(endTo)) {
                        // 获取第二天的 firstFrom
                        ZonedDateTime nextDayFromTime = dateTime.toLocalDate().plusDays(1).atTime(firstFrom).atZone(dateTime.getZone());

                        // 计算当前时间到 nextDayFromTime 的差值
                        Duration timeDifference = Duration.between(dateTime, nextDayFromTime);

                        // 计算可以整除的 duration 数量
                        long n = timeDifference.toMillis() / duration.toMillis();

                        // 跳转到下一个 n * duration 的时间点
                        dateTime = dateTime.plus(n * duration.toMillis(), ChronoUnit.MILLIS);
                    }
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

    private TimerIterable multiSpecIterable() {
        List<TimerSpec> multiSpec = multi.getSpec();
        Assert.notEmpty(multiSpec, "multiSpec can not be empty");
        return baseTime -> new Iterator<ZonedDateTime>() {
            final List<ZonedDateTime> timeList = new ArrayList<>(multiSpec.size());

            final List<Iterator<ZonedDateTime>> iterators = multiSpec
                .stream()
                .map(spec -> spec.iterable().iterator(baseTime))
                .collect(Collectors.toList());

            @Override
            public boolean hasNext() {
                switch (multi.getType()) {
                    case and:
                        return iterators.stream().allMatch(Iterator::hasNext);
                    case or:
                        return iterators.stream().anyMatch(Iterator::hasNext);
                    default:
                        return false;
                }
            }

            @Override
            public ZonedDateTime next() {
                switch (multi.getType()) {
                    case and:
                        return handleNextAnd();
                    case or:
                        return handleNextOr();
                    default:
                        return baseTime;
                }
            }

            private ZonedDateTime handleNextAnd() {
                ZonedDateTime dateTime = null;
                int max = MAX_IT_TIMES;
                int match = 0;
                do {
                    for (Iterator<ZonedDateTime> iterator : iterators) {
                        ZonedDateTime next = iterator.next();
                        if (dateTime == null) {
                            dateTime = next;
                        }
                        // 若生成的时间比当前选中的时间早，则继续生成
                        while (next.isBefore(dateTime)) {
                            next = iterator.next();
                        }
                        if (next.isEqual(dateTime)) {
                            // 所有定时器的next时间一致时，返回时间
                            if (++match == iterators.size()) {
                                return dateTime;
                            }
                        } else {
                            dateTime = next;
                        }
                    }
                    max--;
                } while (
                    max > 0
                );
                return dateTime;
            }

            private ZonedDateTime handleNextOr() {
                ZonedDateTime earliest = null;
                // 每个定时器生成next
                fillTimeList();

                // 获取最早的一个时间
                for (ZonedDateTime zonedDateTime : timeList) {
                    if (earliest == null || earliest.isAfter(zonedDateTime) || earliest.isEqual(zonedDateTime)) {
                        earliest = zonedDateTime;
                    }
                }
                // 清空被选中的最早时间
                for (int i = 0; i < timeList.size(); i++) {
                    if (timeList.get(i).isEqual(earliest)) {
                        timeList.set(i, null);
                    }
                }
                return earliest;
            }

            /**
             * 遍历所有定时器，若有next为空的则生成新的
             */
            private void fillTimeList() {
                for (int i = 0; i < iterators.size(); i++) {
                    if (timeList.size() <= i) {
                        timeList.add(iterators.get(i).next());
                    } else if (timeList.get(i) == null) {
                        timeList.set(i, iterators.get(i).next());
                    }
                }
            }
        };
    }

    public TimerIterable iterable() {
        if (trigger == Trigger.multi) {
            return multiSpecIterable();
        }
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

    @Override
    public String toString() {
        if (getTrigger() == null) {
            return null;
        }
        switch (getTrigger()) {
            case week: {
                return weekDesc();
            }
            case month: {
                return monthDesc();
            }
            case cron: {
               return getCron();
            }
        }
        return null;
    }

    private String weekDesc() {
        I18nSpec spec = new I18nSpec();
        spec.setCode("message.timer_spec_desc");
        List<I18nSpec> args = new ArrayList<>();
        if (when == null || when.isEmpty()) {
            args.add(I18nSpec.of("message.timer_spec_desc_everyday", "每天"));
        } else {
            String week = when
                .stream()
                .map(weekNum -> LocaleUtils.resolveMessage("message.timer_spec_desc_week_" + weekNum))
                .collect(Collectors.joining(LocaleUtils.resolveMessage("message.timer_spec_desc_seperator")));
            args.add(I18nSpec.of(
                "message.timer_spec_desc_everyweek",
                "每周" + week,
                week));
        }
        args.add(timerModDesc());
        spec.setArgs(args);
        return spec.resolveI18nMessage();
    }

    private String monthDesc() {
        I18nSpec spec = new I18nSpec();
        spec.setCode("message.timer_spec_desc");
        List<I18nSpec> args = new ArrayList<>();
        if (when == null || when.isEmpty()) {
            args.add(I18nSpec.of("message.timer_spec_desc_everyday", "每天"));
        } else {
            String month = when
                .stream()
                .map(monthNum -> {
                    switch (monthNum) {
                        case 1:
                            return LocaleUtils.resolveMessage("message.timer_spec_desc_month_1", monthNum);
                        case 2:
                            return LocaleUtils.resolveMessage("message.timer_spec_desc_month_2", monthNum);
                        case 3:
                            return LocaleUtils.resolveMessage("message.timer_spec_desc_month_3", monthNum);
                        default:
                            return LocaleUtils.resolveMessage("message.timer_spec_desc_month", monthNum);
                    }
                })
                .collect(Collectors.joining(LocaleUtils.resolveMessage("message.timer_spec_desc_seperator")));
            args.add(I18nSpec.of(
                "message.timer_spec_desc_everymonth",
                "每月" + month,
                month));
        }
        args.add(timerModDesc());
        spec.setArgs(args);
        return spec.resolveI18nMessage();
    }

    private I18nSpec timerModDesc() {
        switch (getMod()) {
            case period: {
                if (getPeriod() == null) {
                    break;
                }
                return I18nSpec.of(
                    "message.timer_spec_desc_period",
                    getPeriod().getFrom() + "-" + getPeriod().getTo() +
                        " 每" + getPeriod().getEvery() + getPeriod().getUnit().name(),
                    I18nSpec.of("message.timer_spec_desc_period_duration",
                                getPeriod().getFrom() + "-" + getPeriod().getTo(),
                                getPeriod().getFrom(),
                                getPeriod().getTo()),
                    getPeriod().getEvery(),
                    I18nSpec.of("message.timer_spec_desc_period_" + getPeriod().getUnit().name(),
                                getPeriod().getUnit().name())
                );
            }
            case once: {
                // [time]，执行1次
                if (getOnce() == null) {
                    break;
                }
                return I18nSpec.of("message.timer_spec_desc_period_once", getOnce().getTime(), getOnce().getTime());
            }
        }
        return I18nSpec.of("", "");
    }

    @AllArgsConstructor
    static class TimerFlux extends Flux<Long> {
        final Function<ZonedDateTime, Duration> spec;
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
        //按周
        week,
        //按月
        month,
        //cron表达式
        cron,
        // 多个触发组合
        multi
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
