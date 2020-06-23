package org.jetlinks.community.notify.rule;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.core.Values;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
@AllArgsConstructor
public class NotifierTaskExecutorProvider implements TaskExecutorProvider {

    private final NotifierManager notifierManager;

    @Override
    public String getExecutor() {
        return "notifier";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        RuleNotifierProperties properties = FastBeanCopier.copy(context.getJob().getConfiguration(), RuleNotifierProperties.class);
        properties.validate();

        Function<RuleData, Publisher<RuleData>> executor = createExecutor(context, properties);
        return Mono.just(new FunctionTaskExecutor("消息通知", context) {
            @Override
            protected Publisher<RuleData> apply(RuleData input) {
                return executor.apply(input);
            }
        });
    }


    public Function<RuleData, Publisher<RuleData>> createExecutor(ExecutionContext context, RuleNotifierProperties config) {
        return rule -> notifierManager
            .getNotifier(config.getNotifyType(), config.getNotifierId())
            .switchIfEmpty(Mono.fromRunnable(() -> {
                context.getLogger().warn("通知器[{}-{}]不存在", config.getNotifyType(), config.getNotifierId());
            }))
            .flatMap(notifier -> notifier.send(config.getTemplateId(), Values.of(RuleDataHelper.toContextMap(rule))))
            .doOnError(err -> {
                context.getLogger().error("发送[{}]通知[{}-{}]失败",
                    config.getNotifyType().getName(),
                    config.getNotifierId(),
                    config.getTemplateId(), err);
            })
            .doOnSuccess(ignore -> {
                context.getLogger().info("发送[{}]通知[{}-{}]完成",
                    config.getNotifyType().getName(),
                    config.getNotifierId(),
                    config.getTemplateId());
            }).then(Mono.empty());
    }

}
