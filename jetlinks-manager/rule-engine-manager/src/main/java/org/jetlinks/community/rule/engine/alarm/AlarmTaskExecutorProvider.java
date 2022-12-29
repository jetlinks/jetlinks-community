package org.jetlinks.community.rule.engine.alarm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.rule.engine.enums.AlarmMode;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.function.Function;

@AllArgsConstructor
@Component
@Slf4j
public class AlarmTaskExecutorProvider implements TaskExecutorProvider {
    public static final String executor = "alarm";

    private final AlarmRuleHandler alarmHandler;

    @Override
    public String getExecutor() {
        return executor;
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new AlarmTaskExecutor(context, alarmHandler));
    }

    static class AlarmTaskExecutor extends FunctionTaskExecutor {

        private final AlarmRuleHandler handler;

        private Function<RuleData, Flux<AlarmRuleHandler.Result>> executor;

        private Config config;

        public AlarmTaskExecutor(ExecutionContext context, AlarmRuleHandler handler) {
            super("告警", context);
            this.handler = handler;
            reload();
        }

        @Override
        public String getName() {
            return config.getMode() == AlarmMode.relieve
                ? "解除告警" : "触发告警";
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            return executor
                .apply(input)
                .doOnError(err -> log.warn("{} alarm error,rule:{}", config.mode, context.getInstanceId(), err))
                .map(result -> context.newRuleData(input.newData(result.toMap())));
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new Config());
            ValidatorUtils.tryValidate(config);
            if (config.mode == AlarmMode.relieve) {
                executor = input -> handler.relieved(context, input);
            } else {
                executor = input -> handler.triggered(context, input);
            }
        }
    }


    @Getter
    @Setter
    public static class Config implements Serializable {
        @NotNull
        @Schema(description = "告警方式")
        private AlarmMode mode;


    }
}
