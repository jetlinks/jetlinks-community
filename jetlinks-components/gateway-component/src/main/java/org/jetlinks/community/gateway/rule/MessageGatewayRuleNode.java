package org.jetlinks.community.gateway.rule;

import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.gateway.MessageGatewayManager;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Component
public class MessageGatewayRuleNode extends CommonExecutableRuleNodeFactoryStrategy<MessageGatewayRuleNodeConfig> {

    private final MessageGatewayManager gatewayManager;

    static {
        TopicMessageCodec.register();
    }

    public MessageGatewayRuleNode(MessageGatewayManager gatewayManager) {
        this.gatewayManager = gatewayManager;
    }

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, MessageGatewayRuleNodeConfig config) {
        if (config.getType() == PubSubType.consumer) {
            return Mono::just;
        }
        return ruleData -> gatewayManager
            .getGateway(config.getGatewayId())
            .switchIfEmpty(Mono.error(() -> new NotFoundException("消息网关[{" + config.getGatewayId() + "}]不存在")))
            .flatMap(gateway -> config.convert(ruleData)
                .flatMap(msg -> gateway.publish(msg, config.isShareCluster()))
                .then())
            .thenReturn(ruleData);
    }

    @Override
    protected void onStarted(ExecutionContext context, MessageGatewayRuleNodeConfig config) {
        super.onStarted(context, config);
        if (config.getType() == PubSubType.producer) {
            return;
        }
        //订阅网关中的消息
        context.onStop(gatewayManager
            .getGateway(config.getGatewayId())
            .switchIfEmpty(Mono.fromRunnable(() -> context.logger().error("消息网关[{" + config.getGatewayId() + "}]不存在")))
            .flatMapMany(gateway -> gateway.subscribe(config.createTopics()))
            .map(config::convert)
            .flatMap(data -> context.getOutput().write(Mono.just(RuleData.create(data))))
            .onErrorContinue((err, obj) -> {
                context.logger().error(err.getMessage(), err);
            })
            .subscribe()::dispose);

    }

    @Override
    public String getSupportType() {
        return "message-gateway";
    }


}
