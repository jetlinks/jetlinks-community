package org.jetlinks.community.rule.engine.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.TimerSpec;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
public class TimerTaskExecutorProvider implements TaskExecutorProvider {

    @Override
    public String getExecutor() {
        return "timer";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new TimerTaskExecutor(context));
    }

    static class TimerTaskExecutor extends AbstractTaskExecutor {

        TimerSpec spec;

        public TimerTaskExecutor(ExecutionContext context) {
            super(context);
            spec = FastBeanCopier.copy(context.getJob().getConfiguration(), new TimerSpec());
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
            return spec
                .flux()
                .onBackpressureDrop()
                .concatMap(t -> {
                    Map<String, Object> data = new HashMap<>();
                    long currentTime = System.currentTimeMillis();
                    data.put("timestamp", currentTime);
                    data.put("_now", currentTime);
                    data.put("times", t);
                    RuleData ruleData = context.newRuleData(data);
                    return context
                        .getOutput()
                        .write(ruleData)
                        .then(context.fireEvent(RuleConstants.Event.result, ruleData))
                        .onErrorResume(err -> context.onError(err, null).then(Mono.empty()))
                        .as(tracer());
                })
                .subscribe();
        }

        @Override
        public void reload() {
            spec = FastBeanCopier.copy(context.getJob().getConfiguration(), new TimerSpec());
            if (disposable != null) {
                disposable.dispose();
            }
            disposable = doStart();
        }

        @Override
        public void validate() {
            TimerSpec spec = FastBeanCopier.copy(context.getJob().getConfiguration(), new TimerSpec());
            spec.nextDurationBuilder();
            spec.createTimeFilter();
        }

        @Override
        public synchronized void shutdown() {
            super.shutdown();
        }


    }

    public static Flux<ZonedDateTime> getLastExecuteTimes(String cronExpression, Date from, long times) {
        return Flux.defer(() -> Flux
            .fromIterable(TimerSpec
                              .cron(cronExpression)
                              .getNextExecuteTimes(ZonedDateTime.ofInstant(from.toInstant(), ZoneId.systemDefault()), times)));
    }
}
