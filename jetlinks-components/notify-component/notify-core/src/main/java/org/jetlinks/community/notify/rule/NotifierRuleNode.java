package org.jetlinks.community.notify.rule;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Values;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
@AllArgsConstructor
public class NotifierRuleNode extends CommonExecutableRuleNodeFactoryStrategy<RuleNotifierProperties> {

    private NotifierManager notifierManager;

    @Override
    public String getSupportType() {
        return "notifier";
    }

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, RuleNotifierProperties config) {
        return rule -> notifierManager
                .getNotifier(config.getNotifyType(), config.getNotifierId())
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    context.logger().warn("通知器[{}-{}]不存在", config.getNodeType(), config.getNotifierId());
                }))
                .flatMap(notifier -> notifier.send(config.getTemplateId(), Values.of(RuleDataHelper.toContextMap(rule))))
                .doOnError(err -> {
                    context.logger().error("发送[{}]通知[{}-{}]失败",
                            config.getNotifyType().getName(),
                            config.getNotifierId(),
                            config.getTemplateId(), err);
                })
                .doOnSuccess(ignore -> {
                    context.logger().info("发送[{}]通知[{}-{}]完成",
                            config.getNotifyType().getName(),
                            config.getNotifierId(),
                            config.getTemplateId());
                });
    }
}
