/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
@Component
public class DelayTaskExecutorProvider implements TaskExecutorProvider {

    public static final String EXECUTOR = "delay";
    private final Scheduler scheduler;

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
                .create(context, data -> {
                    RuleData ruleData = context.newRuleData(data);
                    return Flux
                        .merge(
                            context.fireEvent(RuleConstants.Event.result, ruleData),
                            context.getOutput().write(ruleData)
                        ).then();
                }, scheduler);
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

        public Disposable create(ExecutionContext context, Function<RuleData, Mono<Void>> handler, Scheduler scheduler) {
            return pauseType.create(this, context, handler, scheduler);
        }

        public static DelayTaskExecutorConfig of(Map<String, Object> configuration) {
            return FastBeanCopier.copy(configuration, new DelayTaskExecutorConfig());
        }
    }

    public enum PauseType {
        delayv {//上游节点指定固定延迟

            @Override
            Disposable create(DelayTaskExecutorConfig config,
                              ExecutionContext context,
                              Function<RuleData, Mono<Void>> handler,
                              Scheduler scheduler) {
                return context
                    .getInput()
                    .accept(data -> {
                        try {
                            Map<String, Object> map = RuleDataHelper.toContextMap(data);
                            if (map.get("delay") == null) {
                                context.getLogger().warn("no delay value from upstream");
                                return Mono.empty();
                            }
                            Duration duration = TimeUtils.parse(String.valueOf(map.get("delay")));
                            context.getLogger().debug("delay execution {} ", duration);
                            return Mono
                                .delay(duration, scheduler)
                                .then(handler.apply(data))
                                .thenReturn(true);
                        } catch (Throwable e) {
                            context
                                .getLogger()
                                .warn("delay execution {} failed", e);
                            return context
                                .onError(e, data);

                        }
                    });
            }

        },
        delay {//固定延迟

            Disposable create(DelayTaskExecutorConfig config,
                              ExecutionContext context,
                              Function<RuleData, Mono<Void>> handler,
                              Scheduler scheduler) {
                Duration duration = Duration.of(config.getTimeout(), config.getTimeoutUnits());
                return context
                    .getInput()
                    .accept(data -> {
                        context.getLogger().debug("delay execution {} ", duration);
                        return Mono
                            .delay(duration, scheduler)
                            .then(handler.apply(data))
                            .thenReturn(true);
                    });
            }

        },
        random {//随机延迟

            @Override
            Disposable create(DelayTaskExecutorConfig config,
                              ExecutionContext context,
                              Function<RuleData, Mono<Void>> handler,
                              Scheduler scheduler) {
                return context
                    .getInput()
                    .accept(data -> {
                        try {
                            Duration duration = Duration.of(
                                ThreadLocalRandom.current().nextLong(
                                    config.getRandomFirst(),
                                    config.getRandomLast()),
                                config.getRandomUnits());
                            context.getLogger().debug("delay execution {} ", duration);
                            return Mono
                                .delay(duration, scheduler)
                                .then(handler.apply(data))
                                .thenReturn(true);
                        } catch (Throwable e) {
                            context
                                .getLogger()
                                .warn("delay execution {} failed", e);
                            return context
                                .onError(e, data);

                        }
                    });
            }
        }
        ;

        abstract Disposable create(DelayTaskExecutorConfig config,
                                   ExecutionContext context,
                                   Function<RuleData, Mono<Void>> handler,
                                   Scheduler scheduler);

    }
}