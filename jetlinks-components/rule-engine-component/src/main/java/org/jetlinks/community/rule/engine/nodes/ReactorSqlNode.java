package org.jetlinks.community.rule.engine.nodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.community.gateway.EncodableMessage;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.events.RuleEvent;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.function.Function;

/**
 * <pre>
 *     {@code
 *
 *     select avg(this.temperature) avgVal, deviceId
 *     from "/device/+/message/property/#"
 *     group _window(10,1) --每10条滚动数据
 *     having avgVal > 10
 *
 *     }
 * </pre>
 */
@Slf4j
@AllArgsConstructor
@Component
public class ReactorSqlNode extends CommonExecutableRuleNodeFactoryStrategy<ReactorSqlNode.Config> {

    private final MessageGateway messageGateway;

    @Override
    public String getSupportType() {
        return "reactor-ql";
    }

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, Config config) {
        ReactorQL ql = config.getReactorQL();

        return data -> ql.start(Flux.just(RuleDataHelper.toContextMap(data)));
    }

    @Override
    protected void onStarted(ExecutionContext context, Config config) {
        log.debug("start reactor ql : {}", config.getSql());
        context.onStop(
            config.getReactorQL()
                .start(table -> {
                    if (table == null || table.equalsIgnoreCase("dual")) {
                        return Flux.just(1);
                    }

                    if (table.startsWith("/")) {
                        return messageGateway
                            .subscribe(
                                Collections.singleton(new Subscription(table)),
                                "rule-engine:".concat(context.getInstanceId()),
                                false)
                            .map(msg -> {
                                //转换为消息
                                if (msg.getMessage() instanceof EncodableMessage) {
                                    return ((EncodableMessage) msg.getMessage()).getNativePayload();
                                }
                                MessagePayloadType payloadType = msg.getMessage().getPayloadType();
                                if (payloadType == null) {
                                    return msg.getMessage().getBytes();
                                }
                                return PayloadType.valueOf(payloadType.name()).read(msg.getMessage().getPayload());
                            });
                    }
                    return Flux.just(1);
                })
                .flatMap(result -> {
                    RuleData data = RuleData.create(result);
                    //输出到下一节点
                    return context.getOutput()
                        .write(Mono.just(RuleData.create(result)))
                        .then(context.fireEvent(RuleEvent.NODE_EXECUTE_DONE, data));
                })
                .onErrorResume(err -> context.onError(RuleData.create(""), err))
                .subscribe()::dispose
        );

    }

    public static class Config implements RuleNodeConfig {

        @Getter
        @Setter
        private String sql;

        private volatile ReactorQL reactorQL;

        @Override
        public NodeType getNodeType() {
            return NodeType.MAP;
        }

        @Override
        public void setNodeType(NodeType nodeType) {

        }

        public ReactorQL getReactorQL() {
            if (reactorQL == null) {
                reactorQL = ReactorQL.builder().sql(sql).build();
            }
            return reactorQL;
        }

        @Override
        public void validate() {
            //不报错就ok
            getReactorQL();
        }
    }

}
