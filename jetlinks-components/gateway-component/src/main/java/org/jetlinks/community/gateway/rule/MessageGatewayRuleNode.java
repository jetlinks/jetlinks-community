package org.jetlinks.community.gateway.rule;

import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.gateway.MessageGatewayManager;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
public class MessageGatewayRuleNode implements TaskExecutorProvider {

    private final MessageGatewayManager gatewayManager;

    static {
        TopicMessageCodec.register();
    }

    public MessageGatewayRuleNode(MessageGatewayManager gatewayManager) {
        this.gatewayManager = gatewayManager;
    }

    @Override
    public String getExecutor() {
        return "message-gateway";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new MessageGatewayPubSubExecutor(context));
    }

    class MessageGatewayPubSubExecutor extends FunctionTaskExecutor {
        MessageGatewayRuleNodeConfig config;

        public MessageGatewayPubSubExecutor(ExecutionContext context) {
            super("消息网关订阅发布", context);
            this.config = FastBeanCopier.copy(context.getJob().getConfiguration(), MessageGatewayRuleNodeConfig.class);
            this.config.validate();
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            return gatewayManager
                .getGateway(config.getGatewayId())
                .switchIfEmpty(Mono.error(() -> new NotFoundException("消息网关[{" + config.getGatewayId() + "}]不存在")))
                .flatMap(gateway -> config.convert(input)
                    .flatMap(msg -> gateway.publish(msg, config.isShareCluster()))
                    .then())
                .thenReturn(input);
        }

        @Override
        protected Disposable doStart() {
            if (config.getType() == PubSubType.producer) {
                return super.doStart();
            }

            //订阅网关中的消息
            return gatewayManager
                .getGateway(config.getGatewayId())
                .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().error("消息网关[{" + config.getGatewayId() + "}]不存在")))
                .flatMapMany(gateway -> gateway.subscribe(config.createTopics()))
                .map(config::convert)
                .flatMap(data -> context.getOutput().write(Mono.just(RuleData.create(data))))
                .onErrorContinue((err, obj) -> {
                    context.getLogger().error(err.getMessage(), err);
                })
                .subscribe();
        }
    }
}
