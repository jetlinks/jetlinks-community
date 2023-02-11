package org.jetlinks.community.rule.engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@AllArgsConstructor
@Component
public class DelayTaskExecutorProvider implements TaskExecutorProvider {

    public static final String EXECUTOR = "delay";
    private final Scheduler scheduler;
    static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getExecutor() {
        return EXECUTOR;
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DelayTaskExecutor(context, scheduler));
    }

    static class DelayTaskExecutor extends AbstractTaskExecutor {

        private DelayTaskExecutorConfig config;

        private final Scheduler scheduler;

        public DelayTaskExecutor(ExecutionContext context, Scheduler scheduler) {
            super(context);
            this.scheduler = scheduler;
            init();
        }

        @Override
        protected Disposable doStart() {
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            return config
                .create(context.getInput().accept(), context, scheduler)
                .map(context::newRuleData)
                .flatMap(ruleData ->
                             context
                                 .fireEvent(RuleConstants.Event.result, ruleData)
                                 .then(context.getOutput().write(Mono.just(ruleData)))
                )
                .onErrorResume(err -> context.onError(err, null))
                .subscribe();
        }

        void init() {
            config = DelayTaskExecutorConfig.of(context.getJob().getConfiguration());
        }

        @Override
        public void reload() {
            init();
            disposable = doStart();
        }

        @Override
        public String getName() {
            return "延迟";
        }

    }

    @Getter
    @Setter
    public static class DelayTaskExecutorConfig {

        //延迟类型
        private PauseType pauseType;

        //延迟
        private int timeout;

        //延迟时间单位
        private ChronoUnit timeoutUnits;

        //速率
        private int rate;

        //速率 单位时间
        private int nbRateUnits;

        //速率 单位
        private ChronoUnit rateUnits;

        //随机延迟从
        private int randomFirst;

        //随机延迟至
        private int randomLast;

        //随机延迟单位
        private ChronoUnit randomUnits;

        //分组表达式
        private String groupExpression;

        //丢弃被限流的消息时触发错误事件
        private boolean errorOnDrop;

        public Flux<RuleData> create(Flux<RuleData> flux, ExecutionContext context, Scheduler scheduler) {
            return pauseType.create(this, flux, context, scheduler);
        }

        public static DelayTaskExecutorConfig of(Map<String, Object> configuration) {
            return FastBeanCopier.copy(configuration, new DelayTaskExecutorConfig());
        }
    }

    public enum PauseType {
        delayv {//上游节点指定固定延迟

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {

                return flux
                    .delayUntil(el -> {
                        Map<String, Object> map = RuleDataHelper.toContextMap(el);
                        if (map.get("delay") == null) {
                            return Mono.never();
                        }
                        Duration duration = TimeUtils.parse(String.valueOf(map.get("delay")));
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono.delay(duration, scheduler);
                    });
            }

        },
        delay {//固定延迟

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {
                return flux
                    .delayUntil(el -> {
                        Duration duration = Duration.of(config.getTimeout(), config.getTimeoutUnits());
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono.delay(duration, scheduler);
                    });
            }

        },
        random {//随机延迟

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {

                return flux
                    .delayUntil(el -> {
                        Duration duration = Duration.of(
                            ThreadLocalRandom.current().nextLong(
                                config.getRandomFirst(),
                                config.getRandomLast()),
                            config.getRandomUnits());
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono.delay(duration, scheduler);
                    });
            }
        },
        rate {//速率限制

            @Override
            Flux<RuleData> create(DelayTaskExecutorConfig config,
                                  Flux<RuleData> flux,
                                  ExecutionContext context,
                                  Scheduler scheduler) {

                Duration duration = Duration.of(config.nbRateUnits, config.getRateUnits());
                return flux
                    .window(duration, scheduler)
                    .flatMap(window -> {
                        AtomicLong counter = new AtomicLong();
                        Flux<RuleData> stream;
                        if (config.isErrorOnDrop()) {//丢弃时触发错误
                            stream = window
                                .index()
                                .flatMap(tp2 -> {
                                    if (tp2.getT1() < config.getRate()) {
                                        return Mono.just(tp2.getT2());
                                    }
                                    return context.fireEvent(RuleConstants.Event.error, context.newRuleData(tp2.getT2()));
                                });
                        } else {
                            stream = window.take(config.getRate());
                        }
                        return stream
                            .doOnNext(v -> counter.incrementAndGet())
                            .doOnComplete(() -> {
                                if (counter.get() > 0) {
                                    context.getLogger().debug("rate limit execution {}/{}", counter, duration);
                                }
                            })
                            ;
                    }, Integer.MAX_VALUE);
            }
        };

        abstract Flux<RuleData> create(DelayTaskExecutorConfig config,
                                       Flux<RuleData> flux,
                                       ExecutionContext context,
                                       Scheduler scheduler);

    }
}
