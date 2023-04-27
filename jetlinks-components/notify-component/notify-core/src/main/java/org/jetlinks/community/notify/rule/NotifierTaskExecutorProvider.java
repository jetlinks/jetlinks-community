package org.jetlinks.community.notify.rule;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.core.Values;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class NotifierTaskExecutorProvider implements TaskExecutorProvider {

    private final NotifierManager notifierManager;

    @Override
    public String getExecutor() {
        return "notifier";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new NotifierTaskExecutor(context));
    }

    class NotifierTaskExecutor extends FunctionTaskExecutor {
        private RuleNotifierProperties properties;

        public NotifierTaskExecutor(ExecutionContext context) {
            super("消息通知", context);
            this.properties = createProperties();
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            return notifierManager
                .getNotifier(properties.getNotifyType(), properties.getNotifierId())
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    context.getLogger().warn("通知配置[{}-{}]不存在", properties.getNotifyType(), properties.getNotifierId());
                }))
                .flatMap(notifier -> notifier.send(properties.getTemplateId(), Values.of(properties.createVariables(input))))
                .doOnError(err -> {
                    context.getLogger().error("发送[{}]通知[{}-{}]失败",
                                              properties.getNotifyType().getName(),
                                              properties.getNotifierId(),
                                              properties.getTemplateId(), err);
                })
                .doOnSuccess(ignore -> {
                    context.getLogger().info("发送[{}]通知[{}-{}]完成",
                                             properties.getNotifyType().getName(),
                                             properties.getNotifierId(),
                                             properties.getTemplateId());
                }).thenReturn(context.newRuleData(input));
        }

        @Override
        public void reload() {
            this.properties = createProperties();
        }

        RuleNotifierProperties createProperties() {
            RuleNotifierProperties properties = FastBeanCopier.copy(context
                                                                        .getJob()
                                                                        .getConfiguration(), RuleNotifierProperties.class);
            properties.initVariable();
            properties.validate();
            return properties;
        }
    }

}
