package org.jetlinks.community.rule.engine.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.TimerSpec;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
public class TimerTaskExecutorProvider implements TaskExecutorProvider {

    private final Scheduler scheduler = Schedulers.parallel();

    @Override
    public String getExecutor() {
        return "timer";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new TimerTaskExecutor(context));
    }

    class TimerTaskExecutor extends AbstractTaskExecutor {

        Supplier<Duration> nextDelay;

        TimerSpec spec;

        Predicate<LocalDateTime> filter;

        public TimerTaskExecutor(ExecutionContext context) {
            super(context);
            nextDelay = createNextDelay();
        }

        @Override
        public String getName() {
            return "定时调度";
        }

        @Override
        protected Disposable doStart() {
            return execute();
        }

        private Disposable execute() {
            Duration nextTime = nextDelay.get();
            context.getLogger().debug("trigger timed task after {}", nextTime);
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            return this.disposable =
                Mono.delay(nextTime, scheduler)
                    .flatMap(t -> {
                        if (!this.filter.test(LocalDateTime.now())) {
                            return Mono.empty();
                        }
                        Map<String, Object> data = new HashMap<>();
                        long currentTime = System.currentTimeMillis();
                        data.put("timestamp", currentTime);
                        data.put("_now", currentTime);
                        return context
                            .getOutput()
                            .write(Mono.just(context.newRuleData(data)))
                            .then(context
                                      .fireEvent(RuleConstants.Event.complete, context.newRuleData(System.currentTimeMillis()))
                                      .thenReturn(1));

                    })
                    .onErrorResume(err -> context.onError(err, null).then(Mono.empty()))
                    .doFinally(s -> {
                        if (getState() == Task.State.running && s != SignalType.CANCEL) {
                            execute();
                        }
                    })
                    .subscribe();
        }

        @Override
        public void reload() {
            nextDelay = createNextDelay();
            disposable = doStart();
        }

        @Override
        public void validate() {
            createNextDelay();
        }

        private Supplier<Duration> createNextDelay() {
            TimerSpec spec = FastBeanCopier.copy(context.getJob().getConfiguration(), new TimerSpec());
            Function<ZonedDateTime, Duration> builder = spec.nextDurationBuilder();
            this.filter = spec.createTimeFilter();
            return () -> builder.apply(ZonedDateTime.now());

        }

    }

    public static Flux<ZonedDateTime> getLastExecuteTimes(String cronExpression, Date from, long times) {
        return Flux.defer(() -> Flux
            .fromIterable(TimerSpec
                              .cron(cronExpression)
                              .getNextExecuteTimes(ZonedDateTime.ofInstant(from.toInstant(), ZoneId.systemDefault()), times)));
    }
}
